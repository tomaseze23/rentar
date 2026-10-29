import { GraphQLError } from 'graphql';
import type { UsuarioToken, Rol } from '../auth';
import { HttpError, toHttpError } from '../errors';
import { alquileres, clientes, vehiculos } from '../grpc/clients'; // antes solo traía clientes
import { clienteDto } from '../mappers';
import { leerFechaHora, validarCliente } from '../validators'; //suma leer fecha
import type { TipoVehiculo, EstadoReserva } from '../dominio';

// Quién hace la consulta (o null si no hay token).
// Es un `type` y no una `interface` porque graphql-http exige un tipo con firma de índice.
export type Contexto = {
  usuario: UsuarioToken | null;
};

// Mismo criterio del Hito 1: el frontend lee errors[].extensions.classification.
function aGraphQLError(err: unknown): GraphQLError {
  const { status, message } = toHttpError(err);
  const classification =
    status === 401 ? 'UNAUTHORIZED' : status === 403 ? 'FORBIDDEN' : status === 404 ? 'NOT_FOUND' : status < 500 ? 'BAD_REQUEST' : 'INTERNAL_ERROR';
  return new GraphQLError(message, { extensions: { classification } });
}

// Envuelve un resolver: traduce cualquier error (HttpError o gRPC) al formato de errores GraphQL.
function resolver<A, R>(fn: (args: A, ctx: Contexto) => Promise<R> | R) {
  return async (args: A, ctx: Contexto): Promise<R> => {
    try {
      return await fn(args, ctx);
    } catch (e) {
      if (toHttpError(e).status >= 500) console.error(e);
      throw aGraphQLError(e);
    }
  };
}

function requerir(ctx: Contexto, ...roles: Rol[]): UsuarioToken {
  if (!ctx.usuario) throw new HttpError(401, 'Se requiere iniciar sesión');
  if (!roles.includes(ctx.usuario.rol)) throw new HttpError(403, 'Acceso denegado');
  return ctx.usuario;
}

const idTexto = (v: unknown, nombre = 'id'): string => {
  const s = String(v ?? '');
  if (!/^\d+$/.test(s)) throw new HttpError(400, `El ${nombre} debe ser numérico`);
  return s;
};

export const rootValue = {
  clientes: resolver(async (args: { incluirInactivos?: boolean }, ctx) => {
    requerir(ctx, 'ADMINISTRADOR');
    const r = await clientes.consultarVarios({ incluirInactivos: args.incluirInactivos === true });
    return r.clientes.map(clienteDto);
  }),

  cliente: resolver(async (args: { id: string }, ctx) => {
    requerir(ctx, 'ADMINISTRADOR');
    return clienteDto(await clientes.consultar({ id: idTexto(args.id) }));
  }),

  crearCliente: resolver(async (args: { input: unknown }, ctx) => {
    requerir(ctx, 'ADMINISTRADOR');
    const d = validarCliente(args.input, true);
    return clienteDto(
      await clientes.crear({
        email: d.email!, password: d.password!, documento: d.documento!, nombre: d.nombre,
        apellido: d.apellido, telefono: d.telefono, fechaNacimiento: d.fechaNacimiento,
      }),
    );
  }),

  actualizarCliente: resolver(async (args: { id: string; input: unknown }, ctx) => {
    requerir(ctx, 'ADMINISTRADOR');
    const d = validarCliente(args.input, false);
    return clienteDto(
      await clientes.actualizar({
        id: idTexto(args.id), nombre: d.nombre, apellido: d.apellido, telefono: d.telefono, fechaNacimiento: d.fechaNacimiento,
      }),
    );
  }),

  eliminarCliente: resolver(async (args: { id: string }, ctx) => {
    requerir(ctx, 'ADMINISTRADOR');
    await clientes.darDeBaja({ id: idTexto(args.id) });
    return true;
  }),

  reactivarCliente: resolver(async (args: { id: string }, ctx) => {
    requerir(ctx, 'ADMINISTRADOR');
    return clienteDto(await clientes.reactivar({ id: idTexto(args.id) }));
  }),

  vehiculosDisponibles: resolver(
    async (
      args: { filtro: { fechaInicio: string; fechaFin: string; tipoVehiculo?: TipoVehiculo; marca?: string; modelo?: string; precioMin?: number; precioMax?: number } },
      ctx,
    ) => {
      requerir(ctx, 'CLIENTE');
      const f = args.filtro;
      const inicio = leerFechaHora(f.fechaInicio, 'inicio');
      const fin = leerFechaHora(f.fechaFin, 'fin');
      if (new Date(fin).getTime() <= new Date(inicio).getTime()) {
        throw new HttpError(400, 'La fecha de fin debe ser posterior a la fecha de inicio');
      }
      if (new Date(inicio).getTime() < Date.now()) throw new HttpError(400, 'La fecha de inicio debe ser futura');
      if (f.precioMin != null && f.precioMax != null && f.precioMin > f.precioMax) {
        throw new HttpError(400, 'El precio mínimo no puede ser mayor que el máximo');
      }

      // 1) Rental: ¿qué vehículos están ocupados en ese período?
      const ocupados = await alquileres.vehiculosOcupados({ periodo: { fechaInicio: inicio, fechaFin: fin } });

      // 2) Vehicle: activos que cumplen los filtros, excluyendo los ocupados.
      const r = await vehiculos.disponibilidad({
        tipoVehiculo: f.tipoVehiculo ?? undefined,
        marca: f.marca ?? undefined,
        modelo: f.modelo ?? undefined,
        precioMin: f.precioMin ?? undefined,
        precioMax: f.precioMax ?? undefined,
        vehiculosOcupadosIds: ocupados.vehiculoIds,
      });

            // Hito 3: un vehículo ROBADO no se ofrece (el Vehicle Service ya lo excluye; esto es defensa en profundidad).
      return r.vehiculos.filter((v) => v.verificacion !== 'ROBADO').map((v) =>({
        id: v.id, patente: v.patente, marca: v.marca, modelo: v.modelo, anio: v.anio,
        color: v.color ?? null, tipoVehiculo: v.tipoVehiculo, precioDiario: v.precioDiario,
      }));
    },
  ),

  reservas: resolver(
    async (
      args: { filtro?: { clienteId?: string; vehiculoId?: string; tipoVehiculo?: TipoVehiculo; estado?: EstadoReserva; fechaDesde?: string; fechaHasta?: string } },
      ctx,
    ) => {
      const u = requerir(ctx, 'CLIENTE', 'ADMINISTRADOR');
      const f = args.filtro ?? {};

      // Un CLIENTE solo ve las suyas; si pide las de otro, FORBIDDEN.
      let clienteId: string | undefined = f.clienteId != null ? idTexto(f.clienteId, 'clienteId') : undefined;
      if (u.rol === 'CLIENTE') {
        if (!u.clienteId) throw new HttpError(403, 'Acceso denegado');
        if (clienteId !== undefined && clienteId !== u.clienteId) throw new HttpError(403, 'Acceso denegado');
        clienteId = u.clienteId;
      }

      const desde = f.fechaDesde ? leerFechaHora(f.fechaDesde, 'desde') : undefined;
      const hasta = f.fechaHasta ? leerFechaHora(f.fechaHasta, 'hasta') : undefined;
      if (desde && hasta && new Date(hasta).getTime() <= new Date(desde).getTime()) {
        throw new HttpError(400, 'fechaHasta debe ser posterior a fechaDesde');
      }

      // Filtro por vehículo y/o tipo: se resuelve a una lista de ids de vehículo.
      // OJO: en el contrato, lista vacía = "sin filtro". Si el filtro no encuentra ningún vehículo, hay que cortar acá.
      let vehiculoIds: string[] = [];
      if (f.vehiculoId != null) vehiculoIds = [idTexto(f.vehiculoId, 'vehiculoId')];
      if (f.tipoVehiculo) {
        const delTipo = await vehiculos.consultarVarios({ tipoVehiculo: f.tipoVehiculo, ids: [] });
        const ids = delTipo.vehiculos.map((v) => v.id);
        vehiculoIds = vehiculoIds.length > 0 ? vehiculoIds.filter((id) => ids.includes(id)) : ids;
        if (vehiculoIds.length === 0) return [];
      }

      const r = await alquileres.consultarReservas({
        clienteId, vehiculoIds, estado: f.estado ?? undefined, fechaDesde: desde, fechaHasta: hasta,
      });
      if (r.reservas.length === 0) return [];

      // Enriquecer con datos de los otros servicios (nombre del cliente, marca/modelo/patente del vehículo).
      const idsV = [...new Set(r.reservas.map((x) => x.vehiculoId))];
      const idsC = [...new Set(r.reservas.map((x) => x.clienteId))];
      const [vs, cs] = await Promise.all([
        vehiculos.consultarVarios({ ids: idsV }),
        Promise.all(idsC.map((id) => clientes.consultar({ id }))),
      ]);
      const vPorId = new Map(vs.vehiculos.map((v) => [v.id, v]));
      const cPorId = new Map(cs.map((c) => [c.id, c]));

      return [...r.reservas]
        .sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio)) // de la más reciente a la más antigua
        .map((x) => {
          const v = vPorId.get(x.vehiculoId);
          const c = cPorId.get(x.clienteId);
          return {
            id: x.id, clienteId: x.clienteId,
            cliente: c ? `${c.nombre} ${c.apellido}` : '',
            vehiculoId: x.vehiculoId,
            vehiculo: v ? `${v.marca} ${v.modelo}` : '',
            patente: v?.patente ?? '',
            fechaInicio: x.fechaInicio, fechaFin: x.fechaFin,
            precioDiario: x.precioDiario.toFixed(2),
            importeTotal: x.importeTotal.toFixed(2),
            estado: x.estado,
            mantenimiento: x.mantenimiento,
          };
        });
    },
  ),

  historialAlquileres: resolver(async (_args: unknown, ctx) => {
    const u = requerir(ctx, 'CLIENTE');
    if (!u.clienteId) throw new HttpError(403, 'Acceso denegado');

    const r = await alquileres.consultarHistorial({ clienteId: u.clienteId });
    if (r.alquileres.length === 0) return [];

    const vs = await vehiculos.consultarVarios({ ids: [...new Set(r.alquileres.map((x) => x.vehiculoId))] });
    const vPorId = new Map(vs.vehiculos.map((v) => [v.id, v]));

    return [...r.alquileres]
      .sort((a, b) => b.fechaInicio.localeCompare(a.fechaInicio)) // del más reciente al más antiguo
      .map((x) => {
        const v = vPorId.get(x.vehiculoId);
        return {
          id: x.reservaId,
          vehiculo: v ? `${v.marca} ${v.modelo}` : '',
          patente: v?.patente ?? '',
          fechaInicio: x.fechaInicio, fechaFin: x.fechaFin,
          cantidadDias: Number(x.cantidadDias),
          importeTotal: x.importeTotal.toFixed(2),
          estado: x.estado,
        };
      });
  }),
};