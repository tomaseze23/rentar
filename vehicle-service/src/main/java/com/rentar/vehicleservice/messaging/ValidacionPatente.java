package com.rentar.vehicleservice.messaging;

/** vehiculos.externos.validacion.patente — resultado: VALIDADO o RECHAZADO. */
public record ValidacionPatente(Long vehiculoExternoId, String patente, String resultado) {
}
