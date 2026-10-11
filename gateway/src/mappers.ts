import type { Vehiculo__Output } from './generated/rentar/vehiculo/v1/Vehiculo';

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