package com.rentar.vehicleservice.services;

import com.rentar.vehicleservice.entities.EstadoSolicitudIncorporacion;
import com.rentar.vehicleservice.entities.ResultadoValidacion;
import com.rentar.vehicleservice.entities.SolicitudIncorporacion;
import com.rentar.vehicleservice.entities.Vehiculo;
import com.rentar.vehicleservice.exceptions.PatenteDuplicadaException;
import com.rentar.vehicleservice.exceptions.SolicitudEnCursoException;
import com.rentar.vehicleservice.exceptions.SolicitudNoEncontradaException;
import com.rentar.vehicleservice.exceptions.VehiculoInvalidoException;
import com.rentar.vehicleservice.messaging.ValidacionCompletada;
import com.rentar.vehicleservice.messaging.VehiculoExternoRegistrado;
import com.rentar.vehicleservice.repositories.SolicitudIncorporacionRepository;
import com.rentar.vehicleservice.repositories.VehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.rentar.vehicleservice.entities.EstadoSolicitudIncorporacion.*;
import static com.rentar.vehicleservice.entities.ResultadoValidacion.*;

/**
 * Incorporación de vehículos de propietarios externos (Hito 3, flujo 2).
 * La solicitud nace PENDIENTE y se anuncia con vehiculos.externos.registrados. Los resultados de patente y
 * titularidad pueden llegar en cualquier orden: si alguno es RECHAZADO la solicitud queda RECHAZADA; si ambos son
 * VALIDADO se publica vehiculos.externos.validacion.completada (una sola vez) y, al consumirlo, se crea el vehículo.
 */
@Service
public class IncorporacionService {

    private static final Logger log = LoggerFactory.getLogger(IncorporacionService.class);

    private final SolicitudIncorporacionRepository solicitudes;
    private final VehiculoRepository vehiculos;
    private final VehiculoDomainService vehiculoService;
    private final ApplicationEventPublisher eventos;

    public IncorporacionService(SolicitudIncorporacionRepository solicitudes, VehiculoRepository vehiculos,
                                VehiculoDomainService vehiculoService, ApplicationEventPublisher eventos) {
        this.solicitudes = solicitudes;
        this.vehiculos = vehiculos;
        this.vehiculoService = vehiculoService;
        this.eventos = eventos;
    }

    @Transactional
    public SolicitudIncorporacion incorporar(SolicitudIncorporacion datos) {
        if (datos.getClienteId() == null || datos.getClienteId() <= 0) {
            throw new VehiculoInvalidoException("El cliente es obligatorio");
        }
        Vehiculo vehiculo = datos.comoVehiculo();
        vehiculoService.normalizarYValidar(vehiculo);
        datos.tomarDatosDe(vehiculo);

        if (vehiculos.existsByPatente(datos.getPatente())) {
            throw new PatenteDuplicadaException(datos.getPatente());
        }
        if (solicitudes.existsByPatenteAndEstado(datos.getPatente(), EstadoSolicitudIncorporacion.PENDIENTE)) {
            throw new SolicitudEnCursoException(datos.getPatente());
        }

        datos.setId(null);
        datos.setEstado(EstadoSolicitudIncorporacion.PENDIENTE);
        datos.setValidacionPatente(ResultadoValidacion.PENDIENTE);
        datos.setValidacionTitularidad(ResultadoValidacion.PENDIENTE);
        datos.setCompletadaPublicada(false);
        datos.setVehiculoId(null);
        datos.setFechaSolicitud(LocalDateTime.now());
        SolicitudIncorporacion guardada = solicitudes.save(datos);

        eventos.publishEvent(new VehiculoExternoRegistrado(guardada.getId(), guardada.getClienteId(), guardada.getPatente()));
        return guardada;
    }

    /** Solicitudes más recientes primero; clienteId null = las de todos los clientes. */
    public List<SolicitudIncorporacion> listar(Long clienteId) {
        if (clienteId == null) {
            return solicitudes.findAllByOrderByIdDesc();
        }
        if (clienteId <= 0) {
            throw new VehiculoInvalidoException("clienteId debe ser mayor a 0");
        }
        return solicitudes.findByClienteIdOrderByIdDesc(clienteId);
    }

    @Transactional
    public void registrarValidacionPatente(Long id, ResultadoValidacion resultado) {
        registrarValidacion(id, resultado, true);
    }

    @Transactional
    public void registrarValidacionTitularidad(Long id, ResultadoValidacion resultado) {
        registrarValidacion(id, resultado, false);
    }

    private void registrarValidacion(Long id, ResultadoValidacion resultado, boolean esPatente) {
        String validacion = esPatente ? "patente" : "titularidad";
        SolicitudIncorporacion s = solicitudes.buscarParaActualizar(id)
                .orElseThrow(() -> new SolicitudNoEncontradaException(id));

        ResultadoValidacion actual = esPatente ? s.getValidacionPatente() : s.getValidacionTitularidad();
        if (actual != ResultadoValidacion.PENDIENTE) {
            log.info("Solicitud {}: resultado de {} duplicado ({}), se ignora", id, validacion, resultado);
            return;
        }
        if (esPatente) {
            s.setValidacionPatente(resultado);
        } else {
            s.setValidacionTitularidad(resultado);
        }

        // Si ya estaba RECHAZADA, el resultado se guarda pero no cambia nada más
        if (s.getEstado() == EstadoSolicitudIncorporacion.PENDIENTE) {
            if (s.getValidacionPatente() == RECHAZADO || s.getValidacionTitularidad() == RECHAZADO) {
                s.setEstado(RECHAZADA);
                log.info("Solicitud {}: {} RECHAZADO -> solicitud RECHAZADA", id, validacion);
            } else if (s.getValidacionPatente() == VALIDADO && s.getValidacionTitularidad() == VALIDADO
                    && !Boolean.TRUE.equals(s.getCompletadaPublicada())) {
                s.setCompletadaPublicada(true);
                eventos.publishEvent(new ValidacionCompletada(id));
                log.info("Solicitud {}: patente y titularidad VALIDADO -> se publica completada", id);
            }
        }
        solicitudes.save(s);
    }

    /** Al consumir completada: crea el vehículo DISPONIBLE a nombre del cliente y la solicitud queda INCORPORADA. */
    @Transactional
    public void completar(Long id) {
        SolicitudIncorporacion s = solicitudes.buscarParaActualizar(id)
                .orElseThrow(() -> new SolicitudNoEncontradaException(id));

        if (s.getEstado() == INCORPORADA) {
            log.info("Solicitud {}: ya estaba INCORPORADA (vehículo {}), se ignora", id, s.getVehiculoId());
            return;
        }
        if (s.getEstado() != EstadoSolicitudIncorporacion.PENDIENTE || s.getValidacionPatente() != VALIDADO || s.getValidacionTitularidad() != VALIDADO) {
            log.warn("Solicitud {}: completada recibido sin ambas validaciones aprobadas (estado {}), se ignora", id, s.getEstado());
            return;
        }
        // Alguien pudo dar de alta la misma patente mientras se validaba
        if (vehiculos.existsByPatente(s.getPatente())) {
            s.setEstado(RECHAZADA);
            solicitudes.save(s);
            log.warn("Solicitud {}: la patente {} ya es un vehículo de la flota -> solicitud RECHAZADA", id, s.getPatente());
            return;
        }

        Vehiculo creado = vehiculoService.incorporarExterno(s.comoVehiculo(), s.getClienteId());
        s.setVehiculoId(creado.getId());
        s.setEstado(INCORPORADA);
        solicitudes.save(s);
        log.info("Solicitud {}: vehículo {} incorporado DISPONIBLE para el cliente {}", id, creado.getId(), s.getClienteId());
    }
}
