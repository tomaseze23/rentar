package com.rental.rentalservice.services;

import com.rental.rentalservice.entities.ResultadoMantenimiento;
import com.rental.rentalservice.entities.SolicitudMantenimiento;
import com.rental.rentalservice.events.MantenimientoResultado;
import com.rental.rentalservice.events.ReservaConfirmada;
import com.rental.rentalservice.repositories.SolicitudMantenimientoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MantenimientoServiceTest {

    @Mock
    private SolicitudMantenimientoRepository solicitudRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // "Ahora" fijo para los tests: 2026-10-01 10:00; anticipación mínima de 24 horas
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);

    private MantenimientoService servicio() {
        return new MantenimientoService(solicitudRepository, eventPublisher, Duration.ofHours(24), RELOJ);
    }

    @Test
    void conAnticipacionSuficiente_registraLaSolicitudYQuedaRealizado() {
        when(solicitudRepository.findByReservaId(15L)).thenReturn(Optional.empty());
        when(solicitudRepository.save(any(SolicitudMantenimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MantenimientoService service = servicio();

        service.procesar(new ReservaConfirmada(
                15, 20, "2026-10-12T10:00:00", "2026-10-13T10:00:00"));

        ArgumentCaptor<SolicitudMantenimiento> solicitud =
                ArgumentCaptor.forClass(SolicitudMantenimiento.class);
        verify(solicitudRepository, times(2)).save(solicitud.capture());
        assertEquals(ResultadoMantenimiento.REALIZADO, solicitud.getValue().getResultado());
        assertEquals(LocalDateTime.parse("2026-10-12T10:00:00"), solicitud.getValue().getFechaInicio());
        verify(eventPublisher).publishEvent(new MantenimientoResultado(15, 20, "REALIZADO"));
    }

    @Test
    void siLaReservaEmpiezaEnMenosDe24Horas_quedaNoRealizado() {
        when(solicitudRepository.findByReservaId(16L)).thenReturn(Optional.empty());
        when(solicitudRepository.save(any(SolicitudMantenimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        servicio().procesar(new ReservaConfirmada(16, 20, "2026-10-01T12:00:00", "2026-10-03T12:00:00"));

        ArgumentCaptor<SolicitudMantenimiento> solicitud = ArgumentCaptor.forClass(SolicitudMantenimiento.class);
        verify(solicitudRepository, times(2)).save(solicitud.capture());
        assertEquals(ResultadoMantenimiento.NO_REALIZADO, solicitud.getValue().getResultado());
        verify(eventPublisher).publishEvent(new MantenimientoResultado(16, 20, "NO_REALIZADO"));
    }

    @Test
    void conExactamente24HorasDeAnticipacion_quedaRealizado() {
        when(solicitudRepository.findByReservaId(17L)).thenReturn(Optional.empty());
        when(solicitudRepository.save(any(SolicitudMantenimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        servicio().procesar(new ReservaConfirmada(17, 20, "2026-10-02T10:00:00", "2026-10-03T10:00:00"));

        verify(eventPublisher).publishEvent(new MantenimientoResultado(17, 20, "REALIZADO"));
    }

    @Test
    void eventoDuplicadoNoCreaOtraSolicitudYRepublicaElResultadoExistente() {
        SolicitudMantenimiento solicitud = solicitudExistente();
        solicitud.setResultado(ResultadoMantenimiento.NO_REALIZADO);
        when(solicitudRepository.findByReservaId(15L)).thenReturn(Optional.of(solicitud));
        MantenimientoService service = servicio();

        service.procesar(new ReservaConfirmada(
                15, 20, "2026-10-12T10:00:00", "2026-10-13T10:00:00"));

        verify(solicitudRepository, never()).save(any());
        verify(eventPublisher).publishEvent(new MantenimientoResultado(15, 20, "NO_REALIZADO"));
    }

    @Test
    void rechazaUnEventoQueNoCoincideConLaSolicitudYaRegistrada() {
        when(solicitudRepository.findByReservaId(15L)).thenReturn(Optional.of(solicitudExistente()));
        MantenimientoService service = servicio();

        assertThrows(IllegalArgumentException.class, () -> service.procesar(new ReservaConfirmada(
                15, 21, "2026-10-12T10:00:00", "2026-10-13T10:00:00")));

        verify(eventPublisher, never()).publishEvent(any());
    }

    private static SolicitudMantenimiento solicitudExistente() {
        return new SolicitudMantenimiento(
                15L, 20L, LocalDateTime.parse("2026-10-12T10:00:00"),
                LocalDateTime.parse("2026-10-13T10:00:00"));
    }
}
