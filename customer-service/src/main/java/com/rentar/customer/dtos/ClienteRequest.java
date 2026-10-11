package com.rentar.customer.dtos;

import java.time.LocalDate;

/** Datos para el alta de un cliente (incluye el email/password de su usuario). */
public record ClienteRequest(
        String email,
        String password,
        String documento,
        String nombre,
        String apellido,
        String telefono,
        LocalDate fechaNacimiento) {
}
