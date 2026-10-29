import * as grpc from '@grpc/grpc-js';
import { config } from '../config';
import { proto } from './loader';
import { aProto, deProto } from '../dominio';
import type {
  TipoVehiculo, EstadoVehiculo, EstadoReserva, EstadoHistorial,
  Verificacion, ResultadoValidacion, EstadoSolicitud, Mantenimiento,
} from '../dominio';


// vehiculos
import type { Vehiculo__Output } from '../generated/rentar/vehiculo/v1/Vehiculo';
import type { IncorporacionExterna__Output } from '../generated/rentar/vehiculo/v1/IncorporacionExterna';
import type { SolicitudIncorporarVehiculoExterno, SolicitudIncorporarVehiculoExterno__Output } from '../generated/rentar/vehiculo/v1/SolicitudIncorporarVehiculoExterno';
import type { RespuestaIncorporarVehiculoExterno, RespuestaIncorporarVehiculoExterno__Output } from '../generated/rentar/vehiculo/v1/RespuestaIncorporarVehiculoExterno';
import type { SolicitudConsultarSolicitudesIncorporacion, SolicitudConsultarSolicitudesIncorporacion__Output } from '../generated/rentar/vehiculo/v1/SolicitudConsultarSolicitudesIncorporacion';
import type { RespuestaConsultarSolicitudesIncorporacion, RespuestaConsultarSolicitudesIncorporacion__Output } from '../generated/rentar/vehiculo/v1/RespuestaConsultarSolicitudesIncorporacion';
import type { SolicitudCrearVehiculo } from '../generated/rentar/vehiculo/v1/SolicitudCrearVehiculo';
import type { SolicitudActualizarVehiculo } from '../generated/rentar/vehiculo/v1/SolicitudActualizarVehiculo';
import type { SolicitudDarDeBajaVehiculo } from '../generated/rentar/vehiculo/v1/SolicitudDarDeBajaVehiculo';
import type { SolicitudReactivarVehiculo } from '../generated/rentar/vehiculo/v1/SolicitudReactivarVehiculo';
import type { SolicitudConsultarVehiculo } from '../generated/rentar/vehiculo/v1/SolicitudConsultarVehiculo';
import type { SolicitudConsultarVehiculos } from '../generated/rentar/vehiculo/v1/SolicitudConsultarVehiculos';
import type { RespuestaConsultarVehiculos__Output } from '../generated/rentar/vehiculo/v1/RespuestaConsultarVehiculos';
import type { SolicitudConsultarDisponibilidad } from '../generated/rentar/vehiculo/v1/SolicitudConsultarDisponibilidad';
import type { RespuestaConsultarDisponibilidad__Output } from '../generated/rentar/vehiculo/v1/RespuestaConsultarDisponibilidad';
import type { SolicitudActualizarEstadoVehiculo } from '../generated/rentar/vehiculo/v1/SolicitudActualizarEstadoVehiculo';


// clientes
import type { Cliente__Output } from '../generated/rentar/cliente/v1/Cliente';
import type { SolicitudCrearCliente } from '../generated/rentar/cliente/v1/SolicitudCrearCliente';
import type { SolicitudActualizarCliente } from '../generated/rentar/cliente/v1/SolicitudActualizarCliente';
import type { SolicitudDarDeBajaCliente } from '../generated/rentar/cliente/v1/SolicitudDarDeBajaCliente';
import type { SolicitudReactivarCliente } from '../generated/rentar/cliente/v1/SolicitudReactivarCliente';
import type { SolicitudConsultarCliente } from '../generated/rentar/cliente/v1/SolicitudConsultarCliente';
import type { SolicitudVerificarClienteActivo } from '../generated/rentar/cliente/v1/SolicitudVerificarClienteActivo';
import type { SolicitudConsultarClientes } from '../generated/rentar/cliente/v1/SolicitudConsultarClientes';
import type { RespuestaConsultarClientes__Output } from '../generated/rentar/cliente/v1/RespuestaConsultarClientes';
import type { RespuestaVerificarClienteActivo__Output } from '../generated/rentar/cliente/v1/RespuestaVerificarClienteActivo';
import type { SolicitudValidarCredencialesLogin } from '../generated/rentar/cliente/v1/SolicitudValidarCredencialesLogin';
import type { RespuestaValidarCredencialesLogin__Output } from '../generated/rentar/cliente/v1/RespuestaValidarCredencialesLogin';


// reservas
import type { Reserva__Output } from '../generated/rentar/alquiler/v1/Reserva';
import type { HistorialAlquiler__Output } from '../generated/rentar/alquiler/v1/HistorialAlquiler';
import type { SolicitudCrearReserva } from '../generated/rentar/alquiler/v1/SolicitudCrearReserva';
import type { SolicitudConsultarReservas } from '../generated/rentar/alquiler/v1/SolicitudConsultarReservas';
import type { RespuestaConsultarReservas__Output } from '../generated/rentar/alquiler/v1/RespuestaConsultarReservas';
import type { SolicitudCancelarReserva } from '../generated/rentar/alquiler/v1/SolicitudCancelarReserva';
import type { SolicitudConsultarHistorial } from '../generated/rentar/alquiler/v1/SolicitudConsultarHistorial';
import type { RespuestaConsultarHistorial__Output } from '../generated/rentar/alquiler/v1/RespuestaConsultarHistorial';
import type { SolicitudConsultarVehiculosOcupados } from '../generated/rentar/alquiler/v1/SolicitudConsultarVehiculosOcupados';
import type { RespuestaConsultarVehiculosOcupados__Output } from '../generated/rentar/alquiler/v1/RespuestaConsultarVehiculosOcupados';



import type { RespuestaCrearVehiculo__Output } from '../generated/rentar/vehiculo/v1/RespuestaCrearVehiculo';
import type { RespuestaActualizarVehiculo__Output } from '../generated/rentar/vehiculo/v1/RespuestaActualizarVehiculo';
import type { RespuestaDarDeBajaVehiculo__Output } from '../generated/rentar/vehiculo/v1/RespuestaDarDeBajaVehiculo';
import type { RespuestaReactivarVehiculo__Output } from '../generated/rentar/vehiculo/v1/RespuestaReactivarVehiculo';
import type { RespuestaConsultarVehiculo__Output } from '../generated/rentar/vehiculo/v1/RespuestaConsultarVehiculo';
import type { RespuestaActualizarEstadoVehiculo__Output } from '../generated/rentar/vehiculo/v1/RespuestaActualizarEstadoVehiculo';
import type { RespuestaCrearCliente__Output } from '../generated/rentar/cliente/v1/RespuestaCrearCliente';
import type { RespuestaActualizarCliente__Output } from '../generated/rentar/cliente/v1/RespuestaActualizarCliente';
import type { RespuestaDarDeBajaCliente__Output } from '../generated/rentar/cliente/v1/RespuestaDarDeBajaCliente';
import type { RespuestaReactivarCliente__Output } from '../generated/rentar/cliente/v1/RespuestaReactivarCliente';
import type { RespuestaConsultarCliente__Output } from '../generated/rentar/cliente/v1/RespuestaConsultarCliente';
import type { RespuestaCrearReserva__Output } from '../generated/rentar/alquiler/v1/RespuestaCrearReserva';
import type { RespuestaCancelarReserva__Output } from '../generated/rentar/alquiler/v1/RespuestaCancelarReserva';

// Convierte un método gRPC (callback) en una función que devuelve una promesa.
function unary<Req, Res>(client: grpc.Client, method: string) {
    return (request: Req): Promise<Res> =>
        new Promise((resolve, reject) => {
            const fn = (client as unknown as Record<string, Function>)[method];
            fn.call(
              client,
              request,
              new grpc.Metadata(),
              { deadline: Date.now() + config.grpcTimeoutMs },
              (err: grpc.ServiceError | null, res: Res) => (err ? reject(err) : resolve(res)),
            );
        });
}

const creds = grpc.credentials.createInsecure();

// Cada RPC tiene su propia respuesta (RespuestaXxx) que trae la entidad en un campo
// ("vehiculo", "cliente", "reserva"). Este módulo la desenvuelve y traduce los enums
// del .proto (TIPO_VEHICULO_SEDAN) a los valores públicos (SEDAN), así el resto del
// gateway no conoce los prefijos.

// ---------- vehículos ----------
export type VehiculoGw = Omit<Vehiculo__Output, 'tipoVehiculo' | 'estado' | 'verificacion'> & {
  tipoVehiculo: TipoVehiculo;
  estado: EstadoVehiculo;
  verificacion: Verificacion;
};
const vehiculoGw = (v: Vehiculo__Output | null | undefined): VehiculoGw => {
  const x = v as Vehiculo__Output;
  return { 
    ...x, 
    tipoVehiculo: deProto.tipo(x.tipoVehiculo), 
    estado: deProto.estadoVehiculo(x.estado),
    verificacion: deProto.verificacion(x.verificacion),
  };
};

// Solicitud de incorporación de un vehículo externo (Hito 3).
export type IncorporacionGw = Omit<
  IncorporacionExterna__Output,
  'tipoVehiculo' | 'estado' | 'validacionPatente' | 'validacionTitularidad'
> & {
  tipoVehiculo: TipoVehiculo;
  estado: EstadoSolicitud;
  validacionPatente: ResultadoValidacion;
  validacionTitularidad: ResultadoValidacion;
};
const incorporacionGw = (i: IncorporacionExterna__Output | null | undefined): IncorporacionGw => {
  const x = i as IncorporacionExterna__Output;
  return {
    ...x,
    tipoVehiculo: deProto.tipo(x.tipoVehiculo),
    estado: deProto.estadoSolicitud(x.estado),
    validacionPatente: deProto.validacion(x.validacionPatente),
    validacionTitularidad: deProto.validacion(x.validacionTitularidad),
  };
};

type ConTipo<T> = Omit<T, 'tipoVehiculo'> & { tipoVehiculo?: TipoVehiculo };

const vClient = new proto.rentar.vehiculo.v1.ServicioVehiculos(config.vehiculoAddr, creds);
const vCrear = unary<SolicitudCrearVehiculo, RespuestaCrearVehiculo__Output>(vClient, 'CrearVehiculo');
const vActualizar = unary<SolicitudActualizarVehiculo, RespuestaActualizarVehiculo__Output>(vClient, 'ActualizarVehiculo');
const vBaja = unary<SolicitudDarDeBajaVehiculo, RespuestaDarDeBajaVehiculo__Output>(vClient, 'DarDeBajaVehiculo');
const vReactivar = unary<SolicitudReactivarVehiculo, RespuestaReactivarVehiculo__Output>(vClient, 'ReactivarVehiculo');
const vConsultar = unary<SolicitudConsultarVehiculo, RespuestaConsultarVehiculo__Output>(vClient, 'ConsultarVehiculo');
const vVarios = unary<SolicitudConsultarVehiculos, RespuestaConsultarVehiculos__Output>(vClient, 'ConsultarVehiculos');
const vDisp = unary<SolicitudConsultarDisponibilidad, RespuestaConsultarDisponibilidad__Output>(vClient, 'ConsultarDisponibilidad');
const vEstado = unary<SolicitudActualizarEstadoVehiculo, RespuestaActualizarEstadoVehiculo__Output>(vClient, 'ActualizarEstadoVehiculo');
const vIncorporar = unary<SolicitudIncorporarVehiculoExterno, RespuestaIncorporarVehiculoExterno__Output>(vClient, 'IncorporarVehiculoExterno');
const vSolicitudes = unary<SolicitudConsultarSolicitudesIncorporacion, RespuestaConsultarSolicitudesIncorporacion__Output>(vClient, 'ConsultarSolicitudesIncorporacion');

export const vehiculos = {
  crear: async (r: ConTipo<SolicitudCrearVehiculo> & { tipoVehiculo: TipoVehiculo }) =>
    vehiculoGw((await vCrear({ ...r, tipoVehiculo: aProto.tipo(r.tipoVehiculo) } as SolicitudCrearVehiculo)).vehiculo),
  actualizar: async (r: ConTipo<SolicitudActualizarVehiculo>) =>
    vehiculoGw((await vActualizar({ ...r, tipoVehiculo: aProto.tipo(r.tipoVehiculo) } as SolicitudActualizarVehiculo)).vehiculo),
  darDeBaja: async (r: SolicitudDarDeBajaVehiculo) => vehiculoGw((await vBaja(r)).vehiculo),
  reactivar: async (r: SolicitudReactivarVehiculo) => vehiculoGw((await vReactivar(r)).vehiculo),
  consultar: async (r: SolicitudConsultarVehiculo) => vehiculoGw((await vConsultar(r)).vehiculo),
  consultarVarios: async (r: ConTipo<SolicitudConsultarVehiculos>) => {
    const res = await vVarios({ ...r, tipoVehiculo: aProto.tipo(r.tipoVehiculo) } as SolicitudConsultarVehiculos);
    return { vehiculos: res.vehiculos.map(vehiculoGw) };
  },
  disponibilidad: async (r: ConTipo<SolicitudConsultarDisponibilidad>) => {
    const res = await vDisp({ ...r, tipoVehiculo: aProto.tipo(r.tipoVehiculo) } as SolicitudConsultarDisponibilidad);
    return { vehiculos: res.vehiculos.map(vehiculoGw) };
  },
  actualizarEstado: async (r: { vehiculoId: string | number; estado: EstadoVehiculo }) =>
    vehiculoGw(
      (await vEstado({ vehiculoId: r.vehiculoId, estado: aProto.estadoVehiculo(r.estado) } as SolicitudActualizarEstadoVehiculo))
        .vehiculo,
    ),
    incorporarExterno: async (r: ConTipo<SolicitudIncorporarVehiculoExterno> & {tipoVehiculo: TipoVehiculo }) =>
      incorporacionGw(
        (await vIncorporar({ ...r, tipoVehiculo: aProto.tipo(r.tipoVehiculo) } as SolicitudIncorporarVehiculoExterno)).solicitud,
      ),
    consultarSolicitudes: async(r: SolicitudConsultarSolicitudesIncorporacion) => {
      const res = await vSolicitudes(r);
      return { solicitudes: res.solicitudes.map(incorporacionGw) };
    },
};

// ---------- clientes ----------
const cClient = new proto.rentar.cliente.v1.ServicioClientes(config.clienteAddr, creds);
const cCrear = unary<SolicitudCrearCliente, RespuestaCrearCliente__Output>(cClient, 'CrearCliente');
const cActualizar = unary<SolicitudActualizarCliente, RespuestaActualizarCliente__Output>(cClient, 'ActualizarCliente');
const cBaja = unary<SolicitudDarDeBajaCliente, RespuestaDarDeBajaCliente__Output>(cClient, 'DarDeBajaCliente');
const cReactivar = unary<SolicitudReactivarCliente, RespuestaReactivarCliente__Output>(cClient, 'ReactivarCliente');
const cConsultar = unary<SolicitudConsultarCliente, RespuestaConsultarCliente__Output>(cClient, 'ConsultarCliente');

export const clientes = {
  crear: async (r: SolicitudCrearCliente) => (await cCrear(r)).cliente as Cliente__Output,
  actualizar: async (r: SolicitudActualizarCliente) => (await cActualizar(r)).cliente as Cliente__Output,
  darDeBaja: async (r: SolicitudDarDeBajaCliente) => (await cBaja(r)).cliente as Cliente__Output,
  reactivar: async (r: SolicitudReactivarCliente) => (await cReactivar(r)).cliente as Cliente__Output,
  consultar: async (r: SolicitudConsultarCliente) => (await cConsultar(r)).cliente as Cliente__Output,
  consultarVarios: unary<SolicitudConsultarClientes, RespuestaConsultarClientes__Output>(cClient, 'ConsultarClientes'),
  verificarActivo: unary<SolicitudVerificarClienteActivo, RespuestaVerificarClienteActivo__Output>(cClient, 'VerificarClienteActivo'),
  validarCredenciales: unary<SolicitudValidarCredencialesLogin, RespuestaValidarCredencialesLogin__Output>(cClient, 'ValidarCredencialesLogin'),
};

// ---------- reservas ----------
export type ReservaGw = Omit<Reserva__Output, 'estado' | 'mantenimiento'> & { 
  estado: EstadoReserva 
  mantenimiento: Mantenimiento;
};
export type HistorialGw = Omit<HistorialAlquiler__Output, 'estado'> & { estado: EstadoHistorial };
const reservaGw = (r: Reserva__Output | null | undefined): ReservaGw => {
  const x = r as Reserva__Output;
  return { ...x, estado: deProto.estadoReserva(x.estado), mantenimiento: deProto.mantenimiento(x.mantenimiento) };
};

const aClient = new proto.rentar.alquiler.v1.ServicioAlquileres(config.alquilerAddr, creds);
const aCrear = unary<SolicitudCrearReserva, RespuestaCrearReserva__Output>(aClient, 'CrearReserva');
const aCancelar = unary<SolicitudCancelarReserva, RespuestaCancelarReserva__Output>(aClient, 'CancelarReserva');
const aVarias = unary<SolicitudConsultarReservas, RespuestaConsultarReservas__Output>(aClient, 'ConsultarReservas');
const aHistorial = unary<SolicitudConsultarHistorial, RespuestaConsultarHistorial__Output>(aClient, 'ConsultarHistorial');

export const alquileres = {
  crearReserva: async (r: SolicitudCrearReserva) => reservaGw((await aCrear(r)).reserva),
  cancelarReserva: async (r: SolicitudCancelarReserva) => reservaGw((await aCancelar(r)).reserva),
  consultarReservas: async (r: Omit<SolicitudConsultarReservas, 'estado'> & { estado?: EstadoReserva }) => {
    const res = await aVarias({ ...r, estado: aProto.estadoReserva(r.estado) } as SolicitudConsultarReservas);
    return { reservas: res.reservas.map(reservaGw) };
  },
  consultarHistorial: async (r: SolicitudConsultarHistorial) => {
    const res = await aHistorial(r);
    return {
      alquileres: res.alquileres.map((h): HistorialGw => ({ ...h, estado: deProto.estadoHistorial(h.estado) })),
    };
  },
  vehiculosOcupados: unary<SolicitudConsultarVehiculosOcupados, RespuestaConsultarVehiculosOcupados__Output>(aClient, 'ConsultarVehiculosOcupados'),
};

export function closeClients() {
    vClient.close();
    cClient.close();
    aClient.close();
}
