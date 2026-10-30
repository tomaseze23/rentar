package com.rentar.vehicleservice.entities;

public enum VerificacionVehiculo {
        PENDIENTE,  // todavía no llegó el resultado
        VALIDADO,   // la patente no figura como robada
        ROBADO      // figura como robada: vehículo inhabilitado
    }

