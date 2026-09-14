package com.rentar.rentar.dtos;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * DTO de entrada para actualizar los datos propios de un Cliente
 * (no incluye email/password, que pertenecen a Usuario).
 */
public class ClienteUpdateRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    private String telefono;

    private LocalDate fechaNacimiento;

    public ClienteUpdateRequest() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }
}
