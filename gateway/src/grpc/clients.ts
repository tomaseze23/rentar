import * as grpc from '@grpc/grpc-js';
import { config } from '../config';
import { proto } from './loader';


// vehiculos
import type { Vehiculo__Output } from '../generated/rentar/vehiculo/v1/Vehiculo';
import type { SolicitudCrearVehiculo } from '../generated/rentar/vehiculo/v1/SolicitudCrearVehiculo';
import type { SolicitudActualizarVehiculo } from '../generated/rentar/vehiculo/v1/SolicitudActualizarVehiculo';
import type { SolicitudIdVehiculo } from '../generated/rentar/vehiculo/v1/SolicitudIdVehiculo';
import type { SolicitudConsultarVehiculos } from '../generated/rentar/vehiculo/v1/SolicitudConsultarVehiculos';
import type { RespuestaConsultarVehiculos__Output } from '../generated/rentar/vehiculo/v1/RespuestaConsultarVehiculos';
import type { SolicitudConsultarDisponibilidad } from '../generated/rentar/vehiculo/v1/SolicitudConsultarDisponibilidad';
import type { RespuestaConsultarDisponibilidad__Output } from '../generated/rentar/vehiculo/v1/RespuestaConsultarDisponibilidad';
import type { SolicitudActualizarEstadoVehiculo } from '../generated/rentar/vehiculo/v1/SolicitudActualizarEstadoVehiculo';


// clientes
import type { Cliente__Output } from '../generated/rentar/cliente/v1/Cliente';
import type { SolicitudCrearCliente } from '../generated/rentar/cliente/v1/SolicitudCrearCliente';
import type { SolicitudActualizarCliente } from '../generated/rentar/cliente/v1/SolicitudActualizarCliente';
import type { SolicitudIdCliente } from '../generated/rentar/cliente/v1/SolicitudIdCliente';
import type { SolicitudConsultarClientes } from '../generated/rentar/cliente/v1/SolicitudConsultarClientes';
import type { RespuestaConsultarClientes__Output } from '../generated/rentar/cliente/v1/RespuestaConsultarClientes';
import type { RespuestaVerificarClienteActivo__Output } from '../generated/rentar/cliente/v1/RespuestaVerificarClienteActivo';
import type { SolicitudValidarCredencialesLogin } from '../generated/rentar/cliente/v1/SolicitudValidarCredencialesLogin';
import type { RespuestaValidarCredencialesLogin__Output } from '../generated/rentar/cliente/v1/RespuestaValidarCredencialesLogin';


// reservas
import type { Reserva__Output } from '../generated/rentar/alquiler/v1/Reserva';
import type { SolicitudCrearReserva } from '../generated/rentar/alquiler/v1/SolicitudCrearReserva';
import type { SolicitudConsultarReservas } from '../generated/rentar/alquiler/v1/SolicitudConsultarReservas';
import type { RespuestaConsultarReservas__Output } from '../generated/rentar/alquiler/v1/RespuestaConsultarReservas';
import type { SolicitudCancelarReserva } from '../generated/rentar/alquiler/v1/SolicitudCancelarReserva';
import type { SolicitudConsultarHistorial } from '../generated/rentar/alquiler/v1/SolicitudConsultarHistorial';
import type { RespuestaConsultarHistorial__Output } from '../generated/rentar/alquiler/v1/RespuestaConsultarHistorial';
import type { SolicitudConsultarVehiculosOcupados } from '../generated/rentar/alquiler/v1/SolicitudConsultarVehiculosOcupados';
import type { RespuestaConsultarVehiculosOcupados__Output } from '../generated/rentar/alquiler/v1/RespuestaConsultarVehiculosOcupados';



// Convierte un método gRPC (callback) en una función que devuelve una promesa.proto
function unary <Req, Res>(client: grpc.Client, method: string) {
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

//bloque vehiculos
const vClient = new proto.rentar.vehiculo.v1.ServicioVehiculos(config.vehiculoAddr, creds);
export const vehiculos = {
  crear: unary<SolicitudCrearVehiculo, Vehiculo__Output>(vClient, 'CrearVehiculo'),
  actualizar: unary<SolicitudActualizarVehiculo, Vehiculo__Output>(vClient, 'ActualizarVehiculo'),
  darDeBaja: unary<SolicitudIdVehiculo, Vehiculo__Output>(vClient, 'DarDeBajaVehiculo'),
  reactivar: unary<SolicitudIdVehiculo, Vehiculo__Output>(vClient, 'ReactivarVehiculo'),
  consultarVarios: unary<SolicitudConsultarVehiculos, RespuestaConsultarVehiculos__Output>(vClient, 'ConsultarVehiculos'),
  consultar: unary<SolicitudIdVehiculo, Vehiculo__Output>(vClient, 'ConsultarVehiculo'),
  disponibilidad: unary<SolicitudConsultarDisponibilidad, RespuestaConsultarDisponibilidad__Output>(vClient, 'ConsultarDisponibilidad'),
  actualizarEstado: unary<SolicitudActualizarEstadoVehiculo, Vehiculo__Output>(vClient, 'ActualizarEstadoVehiculo'),

};

//bloque clientes
const cClient = new proto.rentar.cliente.v1.ServicioClientes(config.clienteAddr, creds);
export const clientes = {
  crear: unary<SolicitudCrearCliente, Cliente__Output>(cClient, 'CrearCliente'),
  actualizar: unary<SolicitudActualizarCliente, Cliente__Output>(cClient, 'ActualizarCliente'),
  darDeBaja: unary<SolicitudIdCliente, Cliente__Output>(cClient, 'DarDeBajaCliente'),
  reactivar: unary<SolicitudIdCliente, Cliente__Output>(cClient, 'ReactivarCliente'),
  consultarVarios: unary<SolicitudConsultarClientes, RespuestaConsultarClientes__Output>(cClient, 'ConsultarClientes'),
  consultar: unary<SolicitudIdCliente, Cliente__Output>(cClient, 'ConsultarCliente'),
  verificarActivo: unary<SolicitudIdCliente, RespuestaVerificarClienteActivo__Output>(cClient, 'VerificarClienteActivo'),
  validarCredenciales: unary<SolicitudValidarCredencialesLogin, RespuestaValidarCredencialesLogin__Output>(cClient, 'ValidarCredencialesLogin'),
};

//bloque reservas
const aClient = new proto.rentar.alquiler.v1.ServicioAlquileres(config.alquilerAddr, creds);
export const alquileres = {
  crearReserva: unary<SolicitudCrearReserva, Reserva__Output>(aClient, 'CrearReserva'),
  consultarReservas: unary<SolicitudConsultarReservas, RespuestaConsultarReservas__Output>(aClient, 'ConsultarReservas'),
  cancelarReserva: unary<SolicitudCancelarReserva, Reserva__Output>(aClient, 'CancelarReserva'),
  consultarHistorial: unary<SolicitudConsultarHistorial, RespuestaConsultarHistorial__Output>(aClient, 'ConsultarHistorial'),
  vehiculosOcupados: unary<SolicitudConsultarVehiculosOcupados, RespuestaConsultarVehiculosOcupados__Output>(aClient, 'ConsultarVehiculosOcupados'),
};

export function closeClients() {
    vClient.close();
    cClient.close();
    aClient.close();
}