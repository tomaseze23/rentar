package com.rental.rentalservice.services;

import com.rental.rentalservice.entities.ResultadoMantenimiento;
import com.rental.rentalservice.entities.SolicitudMantenimiento;
import com.rental.rentalservice.events.MantenimientoResultado;
import com.rental.rentalservice.events.ReservaConfirmada;
import com.rental.rentalservice.repositories.SolicitudMantenimientoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MantenimientoService {

    private final SolicitudMantenimientoRepository solicitudRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MantenimientoService(SolicitudMantenimientoRepository solicitudRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.solicitudRepository = solicitudRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void procesar(ReservaConfirmada evento) {
        validarEvento(evento);
        var fechaInicio = ReservaService.parseFecha(evento.fechaInicio(), "fecha_inicio");
        var fechaFin = ReservaService.parseFecha(evento.fechaFin(), "fecha_fin");
        if (!fechaFin.isAfter(fechaInicio)) {
            throw new IllegalArgumentException("fecha_fin debe ser posterior a fecha_inicio");
        }
        SolicitudMantenimiento solicitud = solicitudRepository.findByReservaId(evento.reservaId())
                .orElseGet(() -> solicitudRepository.save(new SolicitudMantenimiento(
                        evento.reservaId(),
                        evento.vehiculoId(),
                        fechaInicio,
                        fechaFin)));

        if (!solicitud.getVehiculoId().equals(evento.vehiculoId())
                || !solicitud.getFechaInicio().equals(fechaInicio)
                || !solicitud.getFechaFin().equals(fechaFin)) {
            throw new IllegalArgumentException("La reserva confirmada no coincide con la solicitud de mantenimiento");
        }

        if (solicitud.getResultado() == ResultadoMantenimiento.PENDIENTE) {
            solicitud.setResultado(ResultadoMantenimiento.REALIZADO);
            solicitudRepository.save(solicitud);
        }

        eventPublisher.publishEvent(new MantenimientoResultado(
                solicitud.getReservaId(),
                solicitud.getVehiculoId(),
                solicitud.getResultado().name()));
    }

    private static void validarEvento(ReservaConfirmada evento) {
        if (evento == null || evento.reservaId() <= 0 || evento.vehiculoId() <= 0) {
            throw new IllegalArgumentException("reservaId y vehiculoId deben ser mayores que cero");
        }
    }
}
