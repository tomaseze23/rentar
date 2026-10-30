package com.rentar.registrotitularidad;

/** vehiculos.externos.validacion.titularidad — resultado: VALIDADO o RECHAZADO. */
public record ValidacionTitularidad(Long vehiculoExternoId, Long clienteId, String documento,
                                    String patente, String resultado) {
}
