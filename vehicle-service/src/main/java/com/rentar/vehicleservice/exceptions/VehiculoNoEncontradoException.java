package com.rentar.vehicleservice.exceptions;

public class VehiculoNoEncontradoException extends RuntimeException {
    public VehiculoNoEncontradoException(Long id) {
        super("Vehículo no encontrado: " + id);
    }
}