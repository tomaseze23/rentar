package com.rentar.registropolicial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * Incorporación de vehículos externos: consume vehiculos.externos.registrados, consulta el mismo registro de
 * patentes denunciadas y publica vehiculos.externos.validacion.patente (no denunciada → VALIDADO; denunciada →
 * RECHAZADO) con correlation_id = vehiculoExternoId.
 */
@Component
public class ValidacionPatenteListener {

    private static final Logger log = LoggerFactory.getLogger(ValidacionPatenteListener.class);
    private static final String EXCHANGE = "rentar.eventos";
    private static final String EVENTO_RESULTADO = "vehiculos.externos.validacion.patente";

    private final RegistroPatentes registro;
    private final RabbitTemplate rabbitTemplate;

    public ValidacionPatenteListener(RegistroPatentes registro, RabbitTemplate rabbitTemplate) {
        this.registro = registro;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "registro-policial.vehiculos.externos.registrados")
    public void onRegistrado(VehiculoExternoRegistrado evento) {
        // Un mensaje mal formado no se arregla reintentando: va directo a la DLQ
        if (evento == null || evento.vehiculoExternoId() == null || evento.vehiculoExternoId() <= 0
                || evento.patente() == null || evento.patente().isBlank()) {
            throw new AmqpRejectAndDontRequeueException("Evento vehiculos.externos.registrados inválido: " + evento);
        }
        String resultado = registro.estaDenunciada(evento.patente()) ? "RECHAZADO" : "VALIDADO";
        log.info("Solicitud {} patente {} -> {}", evento.vehiculoExternoId(), evento.patente(), resultado);

        ValidacionPatente respuesta = new ValidacionPatente(evento.vehiculoExternoId(), evento.patente(), resultado);
        rabbitTemplate.convertAndSend(EXCHANGE, EVENTO_RESULTADO, respuesta, m -> {
            MessageProperties p = m.getMessageProperties();
            p.setMessageId(UUID.randomUUID().toString());
            p.setType(EVENTO_RESULTADO);
            p.setCorrelationId(String.valueOf(evento.vehiculoExternoId()));
            p.setTimestamp(new Date());
            return m;
        });
    }
}
