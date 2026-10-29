package com.rental.rentalservice.events;

public record ReservaConfirmada(long reservaId, long vehiculoId, String fechaInicio, String fechaFin) {
}
