package com.rentar.rentar.dtos;

import com.rentar.rentar.entities.Cliente;

import java.time.LocalDate;

/**
 * DTO de salida. No expone el password del usuario asociado.
 */
public class ClienteResponse {

    private Long id;
    private Long usuarioId;
    private String email;
    private String documento;
    private String nombre;
    private String apellido;
    private String telefono;
    private LocalDate fechaNacimiento;
    private boolean activo;

    public ClienteResponse() {
    }

    public static ClienteResponse fromEntity(Cliente cliente) {
        ClienteResponse dto = new ClienteResponse();
        dto.id = cliente.getId();
        dto.usuarioId = cliente.getUsuario().getId();
        dto.email = cliente.getUsuario().getEmail();
        dto.documento = cliente.getDocumento();
        dto.nombre = cliente.getNombre();
        dto.apellido = cliente.getApellido();
        dto.telefono = cliente.getTelefono();
        dto.fechaNacimiento = cliente.getFechaNacimiento();
        dto.activo = cliente.isActivo();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getEmail() {
        return email;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public boolean isActivo() {
        return activo;
    }
}
