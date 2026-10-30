package com.rentar.vehicleservice.entities;

public enum EstadoSolicitudIncorporacion {
    PENDIENTE,   // esperando las validaciones de patente y titularidad
    RECHAZADA,   // alguna validación dio RECHAZADO: el vehículo no se incorpora
    INCORPORADA  // ambas dieron VALIDADO y el vehículo ya fue creado
}
