package com.rentar.vehicleservice.exceptions;

public class VehiculoYaActivoException extends RuntimeException {
    public VehiculoYaActivoException(Long id) {
        super("El vehículo ya está activo: " + id);
    }
}