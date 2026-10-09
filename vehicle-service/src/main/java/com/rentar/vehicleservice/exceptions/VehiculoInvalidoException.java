package com.rentar.vehicleservice.exceptions;

public class VehiculoInvalidoException extends RuntimeException {
    public VehiculoInvalidoException(String mensaje) {
        super(mensaje);
    }
}