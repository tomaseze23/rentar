package com.rentar.rentar.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Regla de facturación: cada día o fracción de 24 hs cuenta como un día")
class PeriodoAlquilerTest {

    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 1, 10, 0);

    @Test
    @DisplayName("Menos de un día se factura como un día")
    void menosDeUnDia() {
        assertEquals(1, PeriodoAlquiler.diasFacturados(inicio, inicio.plusMinutes(1)));
        assertEquals(1, PeriodoAlquiler.diasFacturados(inicio, inicio.plusHours(2)));
        assertEquals(1, PeriodoAlquiler.diasFacturados(inicio, inicio.plusHours(23).plusMinutes(59)));
    }

    @Test
    @DisplayName("Múltiplos exactos de 24 hs no suman un día de más")
    void multiplosExactos() {
        assertEquals(1, PeriodoAlquiler.diasFacturados(inicio, inicio.plusHours(24)));
        assertEquals(2, PeriodoAlquiler.diasFacturados(inicio, inicio.plusHours(48)));
        assertEquals(7, PeriodoAlquiler.diasFacturados(inicio, inicio.plusDays(7)));
    }

    @Test
    @DisplayName("Cualquier fracción que excede un múltiplo de 24 hs suma un día, aunque sea de un minuto")
    void fraccionesSumanUnDia() {
        assertEquals(2, PeriodoAlquiler.diasFacturados(inicio, inicio.plusHours(24).plusMinutes(1)));
        assertEquals(2, PeriodoAlquiler.diasFacturados(inicio, inicio.plusHours(24).plusSeconds(1)));
        assertEquals(2, PeriodoAlquiler.diasFacturados(inicio, inicio.plusHours(24).plusNanos(1)));
        assertEquals(2, PeriodoAlquiler.diasFacturados(inicio, inicio.plusHours(25)));
        assertEquals(3, PeriodoAlquiler.diasFacturados(inicio, inicio.plusHours(48).plusMinutes(1)));
    }

    @Test
    @DisplayName("Un período vacío o invertido es inválido")
    void periodoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> PeriodoAlquiler.diasFacturados(inicio, inicio));
        assertThrows(IllegalArgumentException.class, () -> PeriodoAlquiler.diasFacturados(inicio, inicio.minusHours(1)));
    }
}
