package com.rentar.vehicleservice.exceptions;

public class SolicitudNoEncontradaException extends RuntimeException {
    public SolicitudNoEncontradaException(Long id) {
        super("Solicitud de incorporación no encontrada: " + id);
    }
}
