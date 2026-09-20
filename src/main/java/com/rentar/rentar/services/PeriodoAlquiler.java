package com.rentar.rentar.services;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Regla de facturación de un alquiler: cada día, o fracción de día, de 24 horas cuenta como un día completo.
 * Es la única definición de esta regla: la usan tanto el alta de reserva (para el importe) como el historial.
 */
public final class PeriodoAlquiler {

    private PeriodoAlquiler() {
    }

    public static long diasFacturados(LocalDateTime inicio, LocalDateTime fin) {
        Duration duracion = Duration.between(inicio, fin);
        if (duracion.isZero() || duracion.isNegative()) {
            throw new IllegalArgumentException("El período debe tener una duración positiva");
        }
        long diasCompletos = duracion.toDays();
        boolean hayFraccion = !duracion.minusDays(diasCompletos).isZero();
        return hayFraccion ? diasCompletos + 1 : diasCompletos;
    }
}
