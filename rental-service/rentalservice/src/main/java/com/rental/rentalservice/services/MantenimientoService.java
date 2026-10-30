package com.rental.rentalservice.services;

import com.rental.rentalservice.entities.ResultadoMantenimiento;
import com.rental.rentalservice.entities.SolicitudMantenimiento;
import com.rental.rentalservice.events.MantenimientoResultado;
import com.rental.rentalservice.events.ReservaConfirmada;
import com.rental.rentalservice.repositories.SolicitudMantenimientoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Componente de mantenimiento preventivo: registra una solicitud por cada reserva confirmada y la procesa.
 * El mantenimiento se hace antes de entregar el vehículo, así que necesita anticipación: si la reserva empieza en
 * menos de {@code mantenimiento.anticipacion-minima-horas} (24 por defecto) desde que se procesa, no hay tiempo de
 * hacerlo y el resultado es NO_REALIZADO; si no, REALIZADO. La reserva no espera este resultado.
 */
@Service
public class MantenimientoService {

    private static final Logger log = LoggerFactory.getLogger(MantenimientoService.class);

    private final SolicitudMantenimientoRepository solicitudRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration anticipacionMinima;
    private final Clock clock;

    @Autowired
    public MantenimientoService(SolicitudMantenimientoRepository solicitudRepository,
                                ApplicationEventPublisher eventPublisher,
                                @Value("${mantenimiento.anticipacion-minima-horas:24}") long anticipacionMinimaHoras) {
        this(solicitudRepository, eventPublisher, Duration.ofHours(anticipacionMinimaHoras), Clock.systemDefaultZone());
    }

    MantenimientoService(SolicitudMantenimientoRepository solicitudRepository, ApplicationEventPublisher eventPublisher,
                         Duration anticipacionMinima, Clock clock) {
        this.solicitudRepository = solicitudRepository;
        this.eventPublisher = eventPublisher;
        this.anticipacionMinima = anticipacionMinima;
        this.clock = clock;
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

        // Un evento duplicado no vuelve a procesar: se republica el resultado ya registrado
        if (solicitud.getResultado() == ResultadoMantenimiento.PENDIENTE) {
            solicitud.setResultado(resultadoPara(fechaInicio));
            solicitudRepository.save(solicitud);
            log.info("Mantenimiento de la reserva {} (vehículo {}, inicio {}): {}",
                    solicitud.getReservaId(), solicitud.getVehiculoId(), fechaInicio, solicitud.getResultado());
        }

        eventPublisher.publishEvent(new MantenimientoResultado(
                solicitud.getReservaId(),
                solicitud.getVehiculoId(),
                solicitud.getResultado().name()));
    }

    private ResultadoMantenimiento resultadoPara(LocalDateTime fechaInicio) {
        LocalDateTime limite = LocalDateTime.now(clock).plus(anticipacionMinima);
        return fechaInicio.isBefore(limite) ? ResultadoMantenimiento.NO_REALIZADO : ResultadoMantenimiento.REALIZADO;
    }

    private static void validarEvento(ReservaConfirmada evento) {
        if (evento == null || evento.reservaId() <= 0 || evento.vehiculoId() <= 0) {
            throw new IllegalArgumentException("reservaId y vehiculoId deben ser mayores que cero");
        }
    }
}
