package com.rentar.customer.dtos;

import com.rentar.customer.entities.Cliente;

import java.time.LocalDate;

/** Vista de un cliente. No expone el password del usuario asociado. */
public record ClienteResponse(
        Long id,
        Long usuarioId,
        String email,
        String documento,
        String nombre,
        String apellido,
        String telefono,
        LocalDate fechaNacimiento,
        boolean activo) {

    public static ClienteResponse fromEntity(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getUsuario().getId(),
                cliente.getUsuario().getEmail(),
                cliente.getDocumento(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getTelefono(),
                cliente.getFechaNacimiento(),
                cliente.isActivo());
    }
}
