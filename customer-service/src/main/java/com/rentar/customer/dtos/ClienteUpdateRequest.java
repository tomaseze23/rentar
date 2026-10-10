package com.rentar.customer.dtos;

import java.time.LocalDate;

/** Datos propios del cliente que se pueden modificar (no email, documento ni password). */
public record ClienteUpdateRequest(
        String nombre,
        String apellido,
        String telefono,
        LocalDate fechaNacimiento) {
}
