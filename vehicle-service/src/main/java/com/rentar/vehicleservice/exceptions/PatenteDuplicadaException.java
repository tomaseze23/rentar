package com.rentar.vehicleservice.exceptions;

public class PatenteDuplicadaException extends RuntimeException {
    public PatenteDuplicadaException(String patente) {
        super("Ya existe un vehículo con la patente: " + patente);
    }
}