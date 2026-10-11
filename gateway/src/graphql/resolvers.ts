import { GraphQLError } from 'graphql';
import type { UsuarioToken, Rol } from '../auth';
import { HttpError, toHttpError } from '../errors';
import { clientes } from '../grpc/clients';
import { clienteDto } from '../mappers';
import { validarCliente } from '../validators';

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
};