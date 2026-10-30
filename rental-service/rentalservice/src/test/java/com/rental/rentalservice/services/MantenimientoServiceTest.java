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

import java.time.LocalDateTime;
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

    @Test
    void registraLaSolicitudYPublicaElResultadoSimulado() {
        when(solicitudRepository.findByReservaId(15L)).thenReturn(Optional.empty());
        when(solicitudRepository.save(any(SolicitudMantenimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MantenimientoService service = new MantenimientoService(solicitudRepository, eventPublisher);

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
    void eventoDuplicadoNoCreaOtraSolicitudYRepublicaElResultadoExistente() {
        SolicitudMantenimiento solicitud = solicitudExistente();
        solicitud.setResultado(ResultadoMantenimiento.NO_REALIZADO);
        when(solicitudRepository.findByReservaId(15L)).thenReturn(Optional.of(solicitud));
        MantenimientoService service = new MantenimientoService(solicitudRepository, eventPublisher);

        service.procesar(new ReservaConfirmada(
                15, 20, "2026-10-12T10:00:00", "2026-10-13T10:00:00"));

        verify(solicitudRepository, never()).save(any());
        verify(eventPublisher).publishEvent(new MantenimientoResultado(15, 20, "NO_REALIZADO"));
    }

    @Test
    void rechazaUnEventoQueNoCoincideConLaSolicitudYaRegistrada() {
        when(solicitudRepository.findByReservaId(15L)).thenReturn(Optional.of(solicitudExistente()));
        MantenimientoService service = new MantenimientoService(solicitudRepository, eventPublisher);

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
