import type { Vehiculo__Output } from './generated/rentar/vehiculo/v1/Vehiculo';
import type { Cliente__Output } from './generated/rentar/cliente/v1/Cliente';

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