import { status } from '@grpc/grpc-js';
import { proto } from '../grpc/loader';
import { grpcError, serve } from './server';
import type { ServicioVehiculosHandlers } from '../generated/rentar/vehiculo/v1/ServicioVehiculos';
import type { TipoVehiculo } from '../generated/rentar/comun/v1/TipoVehiculo';
import type { EstadoVehiculo } from '../generated/rentar/comun/v1/EstadoVehiculo';
import type { EstadoVerificacion } from '../generated/rentar/vehiculo/v1/EstadoVerificacion';
import type { IncorporacionExterna__Output as IncorporacionExterna } from '../generated/rentar/vehiculo/v1/IncorporacionExterna';

// forma en que el mock guarda los vehículos (todos los campos son obligatorios)
interface Vehiculo {
    id: number; patente: string; marca: string; modelo: string; anio: string; color?: string;
    tipoVehiculo: TipoVehiculo; precioDiario: number; estado: EstadoVehiculo; 
    activo: boolean; verificacion: EstadoVerificacion; propietarioClienteId?: number;
}

const datos: Vehiculo[] = [
    { id: 1, patente: 'AAA123', marca: 'Chevrolet', modelo: 'Cruze', anio: '2022', color: 'Azul', tipoVehiculo: 'TIPO_VEHICULO_SEDAN', precioDiario: 12000, estado: 'ESTADO_VEHICULO_DISPONIBLE', activo: true, verificacion: 'ESTADO_VERIFICACION_VALIDADO' },
    { id: 2, patente: 'AA123BB', marca: 'Ford', modelo: 'Focus', anio: '2020', color: 'Gris', tipoVehiculo: 'TIPO_VEHICULO_SEDAN', precioDiario: 20000, estado: 'ESTADO_VEHICULO_DISPONIBLE', activo: true, verificacion: 'ESTADO_VERIFICACION_VALIDADO' },
    { id: 3, patente: 'AC456DD', marca: 'Toyota', modelo: 'Hilux', anio: '2023', color: 'Blanco', tipoVehiculo: 'TIPO_VEHICULO_PICKUP', precioDiario: 35000, estado: 'ESTADO_VEHICULO_DISPONIBLE', activo: true, verificacion: 'ESTADO_VERIFICACION_VALIDADO' },
    // Vehículo robado: queda inhabilitado (no aparece en disponibilidad y el gateway no deja reservarlo).
    { id: 4, patente: 'ROB123', marca: 'Fiat', modelo: 'Palio', anio: '2015', color: 'Rojo', tipoVehiculo: 'TIPO_VEHICULO_HATCHBACK', precioDiario: 9000, estado: 'ESTADO_VEHICULO_DISPONIBLE', activo: true, verificacion: 'ESTADO_VERIFICACION_ROBADO' },
];
let nextId = 5;

const solicitudes: IncorporacionExterna[] = [];
let nextSolicitud = 1;

function resolver(s: IncorporacionExterna) {
    const robado = s.patente.startsWith('ROB');
    const sinTitularidad = s.patente.startsWith('SIN');
    s.validacionPatente = robado ? 'RESULTADO_VALIDACION_RECHAZADO' : 'RESULTADO_VALIDACION_VALIDADO';
    s.validacionTitularidad = sinTitularidad ? 'RESULTADO_VALIDACION_RECHAZADO' : 'RESULTADO_VALIDACION_VALIDADO';
    if (robado || sinTitularidad) {
        s.estado = 'ESTADO_SOLICITUD_INCORPORACION_RECHAZADA';
        return;
    }
    const v: Vehiculo = {
        id: nextId++, patente: s.patente, marca: s.marca, modelo: s.modelo, anio: s.anio, color: s.color,
        tipoVehiculo: s.tipoVehiculo, precioDiario: s.precioDiario, estado: 'ESTADO_VEHICULO_DISPONIBLE', activo: true,
        verificacion: 'ESTADO_VERIFICACION_VALIDADO', propietarioClienteId: Number(s.clienteId),
    };
    datos.push(v);
    s.estado = 'ESTADO_SOLICITUD_INCORPORACION_INCORPORADA';
    s.vehiculoId = String(v.id);
}

const buscar = (id: string | number) => datos.find((v) => String(v.id) == String(id));
const noEncontrado = (id: string | number) => grpcError(status.NOT_FOUND, `Vehículo no encontrado con ID: ${id}`);
const contiene = (valor: string, buscado?: string) => !buscado || valor.toLowerCase().includes(buscado.trim().toLowerCase());

const handlers: ServicioVehiculosHandlers = {
    CrearVehiculo: (call, cb) => {
        const r = call.request;
        const patente = r.patente.replace(/\s/g, '').toUpperCase();
        if (!patente || !r.marca || !r.modelo) return cb(grpcError(status.INVALID_ARGUMENT, 'Faltan datos obligatorios'));
        if(datos.some((v) => v.patente === patente)){
            return cb(grpcError(status.ALREADY_EXISTS, `Ya existe un vehículo con la patente ${patente}`));
        }
        const v: Vehiculo = {
          id: nextId++, patente, marca: r.marca, modelo: r.modelo, anio: r.anio, color: r.color,
          tipoVehiculo: r.tipoVehiculo, precioDiario: r.precioDiario, estado: 'ESTADO_VEHICULO_DISPONIBLE', 
          activo: true, verificacion: 'ESTADO_VERIFICACION_PENDIENTE',
        };
    datos.push(v);
    cb(null, { vehiculo: v });
    },

    ConsultarVehiculo: (call, cb) => {
        const v = buscar(call.request.id);
        v ? cb(null, { vehiculo: v }) : cb(noEncontrado(call.request.id));
    },

    ConsultarVehiculos: (call, cb) => {
        const r = call.request;
        // lista vacía en 'ids' = sin filtro (así lo define el contrato).
        const vehiculos = datos
            .filter((v) => r.activo == null || v.activo === r.activo)
            .filter((v) => !r.tipoVehiculo || r.tipoVehiculo === 'TIPO_VEHICULO_NO_ESPECIFICADO' || v.tipoVehiculo === r.tipoVehiculo)
            .filter((v) => r.ids.length === 0 || r.ids.includes(String(v.id)));
        cb(null, { vehiculos });
    },

    ActualizarVehiculo: (call, cb) => {
        const r = call.request;
        const v = buscar(r.id);
        if (!v) return cb(noEncontrado(r.id));
        Object.assign(v, {
          marca: r.marca, modelo: r.modelo, anio: r.anio, color: r.color,
          tipoVehiculo: r.tipoVehiculo, precioDiario: r.precioDiario,
        });
        cb(null, { vehiculo: v });
    },

    DarDeBajaVehiculo: (call, cb) => {
        const v = buscar(call.request.id);
        if (!v) return cb(noEncontrado(call.request.id));
        v.activo = false;
        cb(null, { vehiculo: v });
    },

    ReactivarVehiculo: (call, cb) => {
        const v = buscar(call.request.id);
        if (!v) return cb(noEncontrado(call.request.id));
        if (v.activo) return cb(grpcError(status.FAILED_PRECONDITION, `El vehículo ${v.id} ya se encuentra activo`));
        v.activo = true;
        cb(null, { vehiculo: v });
    },

    ConsultarDisponibilidad: (call, cb) => {
        const r = call.request;
        if (r.precioMin != null && r.precioMax != null && r.precioMin > r.precioMax) {
          return cb(grpcError(status.INVALID_ARGUMENT, 'precioMin no puede ser mayor que precioMax'));
        }
        const vehiculos = datos
          .filter((v) => v.activo && v.verificacion !== 'ESTADO_VERIFICACION_ROBADO')
          .filter((v) => !r.tipoVehiculo || r.tipoVehiculo === 'TIPO_VEHICULO_NO_ESPECIFICADO' || v.tipoVehiculo === r.tipoVehiculo)
          .filter((v) => contiene(v.marca, r.marca ?? undefined) && contiene(v.modelo, r.modelo ?? undefined))
          .filter((v) => r.precioMin == null || v.precioDiario >= r.precioMin)
          .filter((v) => r.precioMax == null || v.precioDiario <= r.precioMax)
          .filter((v) => !r.vehiculosOcupadosIds.includes(String(v.id)));
        cb(null, { vehiculos });
    },

        IncorporarVehiculoExterno: (call, cb) => {
        const r = call.request;
        const patente = r.patente.replace(/\s/g, '').toUpperCase();
        if (!patente || !r.marca || !r.modelo) return cb(grpcError(status.INVALID_ARGUMENT, 'Faltan datos obligatorios'));
        if (datos.some((v) => v.patente === patente) || solicitudes.some((s) => s.patente === patente && s.estado === 'ESTADO_SOLICITUD_INCORPORACION_PENDIENTE')) {
            return cb(grpcError(status.ALREADY_EXISTS, `Ya existe un vehículo o una solicitud en curso con la patente ${patente}`));
        }
        const s: IncorporacionExterna = {
            id: String(nextSolicitud++), clienteId: String(r.clienteId), patente, marca: r.marca, modelo: r.modelo, anio: r.anio, color: r.color,
            tipoVehiculo: r.tipoVehiculo, precioDiario: r.precioDiario,
            estado: 'ESTADO_SOLICITUD_INCORPORACION_PENDIENTE',
            validacionPatente: 'RESULTADO_VALIDACION_PENDIENTE', validacionTitularidad: 'RESULTADO_VALIDACION_PENDIENTE',
        };
        solicitudes.push(s);
        setTimeout(() => resolver(s), 3000).unref?.();
        cb(null, { solicitud: s });
    },

    ConsultarSolicitudesIncorporacion: (call, cb) => {
        const r = call.request;
        cb(null, { solicitudes: solicitudes.filter((s) => r.clienteId == null || String(s.clienteId) === String(r.clienteId)) });
    },

} as ServicioVehiculosHandlers;


export const startVehiculoMock = (addr = '0.0.0.0:9091') =>
  serve(addr, proto.rentar.vehiculo.v1.ServicioVehiculos.service, handlers);