import type { IncorporacionGw, VehiculoGw } from './grpc/clients';
import type { Cliente__Output } from './generated/rentar/cliente/v1/Cliente';
import type { ReservaGw } from './grpc/clients';

// solo los campos que el frontend necesita, con los nombres del hito1.
export function vehiculoDto(v: VehiculoGw) {
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
        // Hito 3: resultado de la verificación de la patente. ROBADO inhabilita el vehículo.
        verificacion: v.verificacion,
        propietarioClienteId: v.propietarioClienteId == null ? null : Number (v.propietarioClienteId),
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

export function reservaDto(r: ReservaGw) {
  return {
    id: Number(r.id),
    clienteId: Number(r.clienteId),
    vehiculoId: Number(r.vehiculoId),
    fechaInicio: r.fechaInicio,
    fechaFin: r.fechaFin,
    precioDiario: r.precioDiario,
    importeTotal: r.importeTotal,
    estado: r.estado,
    // Hito 3: resultado del mantenimiento. Asincrono, la reserva no lo espera.
    mantenimiento: r.mantenimiento,
  };
}

// Solicitud de incorporación de un vehículo externo y el avance de cada validación.
export function incorporacionDto(i: IncorporacionGw){
  return {
    id: Number(i.id),
    clienteId: Number(i.clienteId),
    patente: i.patente,
    marca: i.marca,
    modelo: i.modelo,
    anio: i.anio,
    color: i.color ?? null,
    tipoVehiculo: i.tipoVehiculo,
    precioDiario: i.precioDiario,
    estado: i.estado,
    validacionPatente: i.validacionPatente,
    validacionTitularidad: i.validacionTitularidad,
    vehiculoId: i.vehiculoId == null ? null : Number(i.vehiculoId),
  }
}