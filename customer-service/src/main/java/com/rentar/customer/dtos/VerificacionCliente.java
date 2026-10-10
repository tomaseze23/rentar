package com.rentar.customer.dtos;

/** Resultado de verificar un cliente: sólo puede alquilar si existe y está activo. */
public record VerificacionCliente(boolean existe, boolean activo) {
}
