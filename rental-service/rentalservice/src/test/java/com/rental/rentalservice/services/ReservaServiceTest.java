package com.rental.rentalservice.services;

import com.rental.rentalservice.entities.EstadoReserva;
import com.rental.rentalservice.entities.ResultadoMantenimiento;
import com.rental.rentalservice.entities.Reserva;
import com.rental.rentalservice.events.ReservaConfirmada;
import com.rental.rentalservice.repositories.ReservaRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReservaService reservaService;

    @Test
    void creaReservaCalculandoDiasCompletosHaciaArriba() {
        when(reservaRepository.existsConfirmedOverlap(eq(20L), any(), any())).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> {
            Reserva reserva = invocation.getArgument(0);
            reserva.setId(30L);
            return reserva;
        });

        Reserva reserva = reservaService.crearReserva(
                10, 20, "2026-10-12T00:00:00", "2026-10-13T01:00:00", 125.50);

        assertEquals(new BigDecimal("125.50"), reserva.getPrecioDiario());
        assertEquals(new BigDecimal("251.00"), reserva.getImporteTotal());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        assertEquals(ResultadoMantenimiento.PENDIENTE, reserva.getResultadoMantenimiento());
        verify(eventPublisher).publishEvent(new ReservaConfirmada(
                30, 20, "2026-10-12T00:00:00", "2026-10-13T01:00:00"));
    }

    @Test
    void aplicaResultadoDeMantenimientoUnaSolaVez() {
        Reserva reserva = reservaFutura(30, 10);
        when(reservaRepository.findById(30L)).thenReturn(Optional.of(reserva));

        reservaService.registrarResultadoMantenimiento(
                new com.rental.rentalservice.events.MantenimientoResultado(30, 20, "REALIZADO"));
        reservaService.registrarResultadoMantenimiento(
                new com.rental.rentalservice.events.MantenimientoResultado(30, 20, "REALIZADO"));

        assertEquals(ResultadoMantenimiento.REALIZADO, reserva.getResultadoMantenimiento());
        verify(reservaRepository, times(1)).save(reserva);
    }

    @Test
    void rechazaSolapamientoDeReservaConfirmada() {
        when(reservaRepository.existsConfirmedOverlap(eq(20L), any(), any())).thenReturn(true);

        StatusRuntimeException error = assertThrows(StatusRuntimeException.class, () ->
                reservaService.crearReserva(10, 20, "2026-10-12T00:00:00", "2026-10-13T00:00:00", 100));

        assertEquals(Status.Code.FAILED_PRECONDITION, Status.fromThrowable(error).getCode());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void rechazaFechasInvertidasComoArgumentoInvalido() {
        StatusRuntimeException error = assertThrows(StatusRuntimeException.class, () ->
                reservaService.crearReserva(10, 20, "2026-10-13T00:00:00", "2026-10-12T00:00:00", 100));

        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(error).getCode());
        verify(reservaRepository, never()).existsConfirmedOverlap(any(), any(), any());
    }

    @Test
    void cancelarReservaExigeQuePertenezcaAlCliente() {
        Reserva reserva = reservaFutura(30, 10);
        when(reservaRepository.findById(30L)).thenReturn(Optional.of(reserva));

        StatusRuntimeException error = assertThrows(StatusRuntimeException.class, () ->
                reservaService.cancelarReserva(30, 11));

        assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(error).getCode());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cancelarReservaRechazaUnaReservaQueYaComenzo() {
        Reserva reserva = reservaFutura(30, 10);
        reserva.setFechaInicio(LocalDateTime.now().minusMinutes(1));
        when(reservaRepository.findById(30L)).thenReturn(Optional.of(reserva));

        StatusRuntimeException error = assertThrows(StatusRuntimeException.class, () ->
                reservaService.cancelarReserva(30, 10));

        assertEquals(Status.Code.FAILED_PRECONDITION, Status.fromThrowable(error).getCode());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cancelarReservaInexistenteDevuelveNotFound() {
        when(reservaRepository.findById(30L)).thenReturn(Optional.empty());

        StatusRuntimeException error = assertThrows(StatusRuntimeException.class, () ->
                reservaService.cancelarReserva(30, 10));

        assertEquals(Status.Code.NOT_FOUND, Status.fromThrowable(error).getCode());
    }

    private static Reserva reservaFutura(long id, long clienteId) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setClienteId(clienteId);
        reserva.setVehiculoId(20L);
        reserva.setFechaInicio(LocalDateTime.now().plusDays(1));
        reserva.setFechaFin(LocalDateTime.now().plusDays(2));
        reserva.setPrecioDiario(BigDecimal.valueOf(100));
        reserva.setImporteTotal(BigDecimal.valueOf(100));
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reserva;
    }
}
