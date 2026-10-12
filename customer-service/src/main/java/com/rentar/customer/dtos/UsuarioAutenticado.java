package com.rentar.customer.dtos;

/** Usuario cuyas credenciales fueron validadas. clienteId es null si no es cliente (ej: admin). */
public record UsuarioAutenticado(
        Long usuarioId,
        String email,
        String rol,
        Long clienteId) {
}
