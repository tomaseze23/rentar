package com.rentar.vehicleservice.messaging;

import com.rentar.vehicleservice.entities.ResultadoValidacion;
import com.rentar.vehicleservice.exceptions.SolicitudNoEncontradaException;
import com.rentar.vehicleservice.services.IncorporacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consume los resultados de las validaciones de patente y titularidad, y el evento completada.
 * Un mensaje que no se puede procesar (id inexistente, resultado inválido) no se arregla reintentando: va a la DLQ.
 */
@Component
public class IncorporacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(IncorporacionConsumer.class);

    private final IncorporacionService service;

    public IncorporacionConsumer(IncorporacionService service) {
        this.service = service;
    }

    @RabbitListener(queues = "vehicle-service.vehiculos.externos.validacion.patente")
    public void onValidacionPatente(ValidacionPatente evento) {
        Long id = idDe(evento == null ? null : evento.vehiculoExternoId(), evento);
        ResultadoValidacion resultado = parsear(evento.resultado());
        log.info("Validación de patente de la solicitud {}: {}", id, resultado);
        conSolicitud(id, () -> service.registrarValidacionPatente(id, resultado));
    }

    @RabbitListener(queues = "vehicle-service.vehiculos.externos.validacion.titularidad")
    public void onValidacionTitularidad(ValidacionTitularidad evento) {
        Long id = idDe(evento == null ? null : evento.vehiculoExternoId(), evento);
        ResultadoValidacion resultado = parsear(evento.resultado());
        log.info("Validación de titularidad de la solicitud {}: {}", id, resultado);
        conSolicitud(id, () -> service.registrarValidacionTitularidad(id, resultado));
    }

    @RabbitListener(queues = "vehicle-service.vehiculos.externos.validacion.completada")
    public void onCompletada(ValidacionCompletada evento) {
        Long id = idDe(evento == null ? null : evento.vehiculoExternoId(), evento);
        log.info("Validaciones completas de la solicitud {}: se incorpora el vehículo", id);
        conSolicitud(id, () -> service.completar(id));
    }

    private static Long idDe(Long id, Object evento) {
        if (id == null || id <= 0) {
            throw new AmqpRejectAndDontRequeueException("Evento sin vehiculoExternoId válido: " + evento);
        }
        return id;
    }

    private static ResultadoValidacion parsear(String resultado) {
        if ("VALIDADO".equals(resultado)) return ResultadoValidacion.VALIDADO;
        if ("RECHAZADO".equals(resultado)) return ResultadoValidacion.RECHAZADO;
        throw new AmqpRejectAndDontRequeueException("Resultado de validación inválido: " + resultado);
    }

    private static void conSolicitud(Long id, Runnable accion) {
        try {
            accion.run();
        } catch (SolicitudNoEncontradaException e) {
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}
