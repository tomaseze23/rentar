import { status } from '@grpc/grpc-js';
import { proto } from '../grpc/loader';
import { grpcError, serve } from './server';
import type { ServicioVehiculosHandlers } from '../generated/rentar/vehiculo/v1/ServicioVehiculos';
import type { TipoVehiculo } from '../generated/rentar/comun/v1/TipoVehiculo';
import type { EstadoVehiculo } from '../generated/rentar/comun/v1/EstadoVehiculo';

// forma en que el mock guarda los vehículos (todos los campos son obligatorios)
interface Vehiculo {
    id: number; patente: string; marca: string; modelo: string; anio: string; color?: string;
    tipoVehiculo: TipoVehiculo; precioDiario: number; estado: EstadoVehiculo; activo: boolean;
}

const datos: Vehiculo[] = [
    { id: 1, patente: 'AAA123', marca: 'Chevrolet', modelo: 'Cruze', anio: '2022', color: 'Azul', tipoVehiculo: 'SEDAN', precioDiario: 12000, estado: 'DISPONIBLE', activo: true },
    { id: 2, patente: 'AA123BB', marca: 'Ford', modelo: 'Focus', anio: '2020', color: 'Gris', tipoVehiculo: 'SEDAN', precioDiario: 20000, estado: 'DISPONIBLE', activo: true },
    { id: 3, patente: 'AC456DD', marca: 'Toyota', modelo: 'Hilux', anio: '2023', color: 'Blanco', tipoVehiculo: 'PICKUP', precioDiario: 35000, estado: 'DISPONIBLE', activo: true },
];
let nextId = 4;

const buscar = (id: string | number) => datos.find((v) => String(v.id) == String(id));
const noEncontrado = (id: string | number) => grpcError(status.NOT_FOUND, `Vehículo no encontrado con ID: ${id}`);

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
          tipoVehiculo: r.tipoVehiculo, precioDiario: r.precioDiario, estado: 'DISPONIBLE', activo: true,
        };
    datos.push(v);
    cb(null, v);
    },

    ConsultarVehiculo: (call, cb) => {
        const v = buscar(call.request.id);
        v ? cb(null, v) : cb(noEncontrado(call.request.id));
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

    //los demás métodos se agrgean después, igual que estos.
} as ServicioVehiculosHandlers;


export const startVehiculoMock = (addr = '0.0.0.0:9091') =>
  serve(addr, proto.rentar.vehiculo.v1.ServicioVehiculos.service, handlers);