package com.rental.rentalservice;

import com.rental.rentalservice.entities.EstadoReserva;
import com.rental.rentalservice.entities.Reserva;
import com.rental.rentalservice.repositories.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rentalservice;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RentalserviceApplicationTests {

    @Autowired
    private ReservaRepository reservaRepository;

    @BeforeEach
    void limpiarReservas() {
        reservaRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void superposicionUsaIntervalosAbiertosYSoloReservasConfirmadas() {
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 12, 10, 0);
        reservaRepository.saveAndFlush(new Reserva(
                null, 1L, 2L, inicio, inicio.plusHours(2),
                BigDecimal.valueOf(100), BigDecimal.valueOf(100), EstadoReserva.CONFIRMADA));

        assertTrue(reservaRepository.existsConfirmedOverlap(2L, inicio.plusMinutes(30), inicio.plusHours(3)));
        assertFalse(reservaRepository.existsConfirmedOverlap(2L, inicio.plusHours(2), inicio.plusHours(3)));
        assertFalse(reservaRepository.existsConfirmedOverlap(3L, inicio.plusMinutes(30), inicio.plusHours(3)));
    }
}
