package com.rentar.vehicleservice.exceptions;

public class SolicitudEnCursoException extends RuntimeException {
    public SolicitudEnCursoException(String patente) {
        super("Ya hay una solicitud de incorporación en curso para la patente: " + patente);
    }
}
