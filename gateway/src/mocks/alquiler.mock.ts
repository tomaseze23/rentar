import { status } from '@grpc/grpc-js';
import { proto } from '../grpc/loader';
import { grpcError, serve } from './server';
import type { ServicioAlquileresHandlers } from '../generated/rentar/alquiler/v1/ServicioAlquileres';
import type { EstadoReserva } from '../generated/rentar/comun/v1/EstadoReserva';
import type { ResultadoMantenimiento } from '../generated/rentar/alquiler/v1/ResultadoMantenimiento';


interface Reserva {
  id: number; clienteId: number; vehiculoId: number; fechaInicio: string; fechaFin: string;
  precioDiario: number; importeTotal: number; estado: EstadoReserva; mantenimiento: ResultadoMantenimiento;
}

// Fechas en formato yyyy-MM-dd'T'HH:mm:ss (sin zona horaria).
const reservas: Reserva[] = [
  // Reserva pasada: en el historial aparece como FINALIZADA (se deriva por fecha, no se guarda).
  { id: 1, clienteId: 1, vehiculoId: 1, fechaInicio: '2026-09-01T10:00:00', fechaFin: '2026-09-03T10:00:00', precioDiario: 12000, importeTotal: 24000, estado: 'ESTADO_RESERVA_CONFIRMADA', mantenimiento: 'RESULTADO_MANTENIMIENTO_REALIZADO' },
];
let nextId = 2;

const MS_DIA = 24 * 60 * 60 * 1000;
const t = (s: string) => new Date(s).getTime();
// Dos períodos se solapan si: inicioA < finB  Y  inicioB < finA
const solapa = (aIni: string, aFin: string, bIni: string, bFin: string) => t(aIni) < t(bFin) && t(bIni) < t(aFin);
// 24 h o fracción = 1 día completo (misma regla de facturación del Hito 1)
const dias = (ini: string, fin: string) => Math.ceil((t(fin) - t(ini)) / MS_DIA);

const handlers: ServicioAlquileresHandlers = {
  CrearReserva: (call, cb) => {
    const r = call.request;
    const ini = r.periodo?.fechaInicio, fin = r.periodo?.fechaFin;
    if (!ini || !fin || isNaN(t(ini)) || isNaN(t(fin))) return cb(grpcError(status.INVALID_ARGUMENT, 'Fechas inválidas'));
    if (t(fin) <= t(ini)) return cb(grpcError(status.INVALID_ARGUMENT, 'La fecha de fin debe ser posterior a la de inicio'));
    if (t(ini) < Date.now()) return cb(grpcError(status.INVALID_ARGUMENT, 'La reserva debe comenzar en el futuro'));
    const ocupado = reservas.some(
      (x) => x.vehiculoId === Number(r.vehiculoId) && x.estado === 'ESTADO_RESERVA_CONFIRMADA' && solapa(x.fechaInicio, x.fechaFin, ini, fin),
    );
    if (ocupado) return cb(grpcError(status.FAILED_PRECONDITION, 'El vehículo ya cuenta con una reserva confirmada en el período indicado'));
    const nueva: Reserva = {
      id: nextId++, clienteId: Number(r.clienteId), vehiculoId: Number(r.vehiculoId), fechaInicio: ini, fechaFin: fin,
      precioDiario: r.precioDiario, importeTotal: dias(ini, fin) * r.precioDiario, 
      estado: 'ESTADO_RESERVA_CONFIRMADA', mantenimiento: 'RESULTADO_MANTENIMIENTO_PENDIENTE',
    };
    reservas.push(nueva);
    cb(null, { reserva: nueva });
  },

  CancelarReserva: (call, cb) => {
    const x = reservas.find((r) => String(r.id) === String(call.request.reservaId));
    if (!x) return cb(grpcError(status.NOT_FOUND, `Reserva no encontrada con ID: ${call.request.reservaId}`));
    if (String(x.clienteId) !== String(call.request.clienteId)) return cb(grpcError(status.PERMISSION_DENIED, 'No podés cancelar reservas de otro cliente'));
    if (x.estado === 'ESTADO_RESERVA_CANCELADA') return cb(grpcError(status.FAILED_PRECONDITION, 'La reserva ya se encuentra cancelada'));
    if (t(x.fechaInicio) <= Date.now()) return cb(grpcError(status.FAILED_PRECONDITION, 'No se puede cancelar una reserva cuyo período ya comenzó'));
    x.estado = 'ESTADO_RESERVA_CANCELADA';
    cb(null, { reserva: x });
  },

  ConsultarReservas: (call, cb) => {
    const r = call.request;
    // Lista vacía en `vehiculoIds` = sin filtro (así lo define el contrato).
    const res = reservas
      .filter((x) => r.clienteId == null || String(x.clienteId) === String(r.clienteId))
      .filter((x) => r.vehiculoIds.length === 0 || r.vehiculoIds.includes(String(x.vehiculoId)))
      .filter((x) => !r.estado || r.estado === 'ESTADO_RESERVA_NO_ESPECIFICADO' || x.estado === r.estado)
      .filter((x) => !r.fechaDesde || t(x.fechaFin) > t(r.fechaDesde))
      .filter((x) => !r.fechaHasta || t(x.fechaInicio) < t(r.fechaHasta));
    cb(null, { reservas: res });
  },

  ConsultarHistorial: (call, cb) => {
    const ahora = Date.now();
    const alquileres = reservas
      .filter((x) => String(x.clienteId) === String(call.request.clienteId))
      .filter((x) => x.estado === 'ESTADO_RESERVA_CANCELADA' || t(x.fechaFin) < ahora)
      .map((x) => ({
        reservaId: x.id, vehiculoId: x.vehiculoId, fechaInicio: x.fechaInicio, fechaFin: x.fechaFin,
        cantidadDias: dias(x.fechaInicio, x.fechaFin), importeTotal: x.importeTotal,
        estado: x.estado === 'ESTADO_RESERVA_CANCELADA' ? ('ESTADO_HISTORIAL_CANCELADA' as const) : ('ESTADO_HISTORIAL_FINALIZADA' as const),
      }));
    cb(null, { alquileres });
  },

  ConsultarVehiculosOcupados: (call, cb) => {
    const ini = call.request.periodo?.fechaInicio, fin = call.request.periodo?.fechaFin;
    if (!ini || !fin || t(fin) <= t(ini)) return cb(grpcError(status.INVALID_ARGUMENT, 'Período inválido'));
    const ids = reservas
      .filter((x) => x.estado === 'ESTADO_RESERVA_CONFIRMADA' && solapa(x.fechaInicio, x.fechaFin, ini, fin))
      .map((x) => x.vehiculoId);
    cb(null, { vehiculoIds: [...new Set(ids)] });
  },
};

export const startAlquilerMock = (addr = '0.0.0.0:9093') =>
  serve(addr, proto.rentar.alquiler.v1.ServicioAlquileres.service, handlers);