import type { Vehiculo__Output } from './generated/rentar/vehiculo/v1/Vehiculo';
import type { Cliente__Output } from './generated/rentar/cliente/v1/Cliente';
import type { Reserva__Output } from './generated/rentar/alquiler/v1/Reserva';

// solo los campos que el frontend necesita, con los nombres del hito1.
export function vehiculoDto(v: Vehiculo__Output) {
    return {
        id: Number(v.id),
        patente: v.patente,
        marca: v.marca,
        modelo: v.modelo,
        anio: v.anio,
        color: v.color ?? null,
        tipoVehiculo: v.tipoVehiculo,
        precioDiario: v.precioDiario,
        estado: v.estado,
        activo: v.activo,
    };
}

export function clienteDto(c: Cliente__Output) {
  return {
    id: Number(c.id),
    usuarioId: Number(c.usuarioId),
    email: c.email,
    documento: c.documento,
    nombre: c.nombre,
    apellido: c.apellido,
    telefono: c.telefono ?? null,
    fechaNacimiento: c.fechaNacimiento ?? null,
    activo: c.activo,
  };
}

export function reservaDto(r: Reserva__Output) {
  return {
    id: Number(r.id),
    clienteId: Number(r.clienteId),
    vehiculoId: Number(r.vehiculoId),
    fechaInicio: r.fechaInicio,
    fechaFin: r.fechaFin,
    precioDiario: r.precioDiario,
    importeTotal: r.importeTotal,
    estado: r.estado,
  };
}