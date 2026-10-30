package com.rentar.registropolicial;

/** vehiculos.verificacion.resultado — resultado: VALIDADO o ROBADO. */
public record VerificacionResultado(Long vehiculoId, String patente, String resultado) {
}
