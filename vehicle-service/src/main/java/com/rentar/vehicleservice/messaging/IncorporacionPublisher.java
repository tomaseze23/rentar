package com.rentar.vehicleservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Date;
import java.util.UUID;

/**
 * Publica los eventos de la incorporación de vehículos externos después del commit, para no anunciar una solicitud
 * (o un cambio de estado) que después se deshace. correlation_id = vehiculoExternoId.
 */
@Component
public class IncorporacionPublisher {

    private static final Logger log = LoggerFactory.getLogger(IncorporacionPublisher.class);
    private static final String EXCHANGE = "rentar.eventos";
    static final String EVENTO_REGISTRADOS = "vehiculos.externos.registrados";
    static final String EVENTO_COMPLETADA = "vehiculos.externos.validacion.completada";

    private final RabbitTemplate rabbitTemplate;

    public IncorporacionPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alRegistrarseUnaSolicitud(VehiculoExternoRegistrado evento) {
        publicar(EVENTO_REGISTRADOS, evento.vehiculoExternoId(), evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alAprobarseLasValidaciones(ValidacionCompletada evento) {
        publicar(EVENTO_COMPLETADA, evento.vehiculoExternoId(), evento);
    }

    private void publicar(String tipo, Long vehiculoExternoId, Object evento) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, tipo, evento, m -> {
                MessageProperties p = m.getMessageProperties();
                p.setMessageId(UUID.randomUUID().toString());
                p.setType(tipo);
                p.setCorrelationId(String.valueOf(vehiculoExternoId));
                p.setTimestamp(new Date());
                return m;
            });
            log.info("Publicado {} para la solicitud {}", tipo, vehiculoExternoId);
        } catch (AmqpException e) {
            log.error("No se pudo publicar {} para la solicitud {}", tipo, vehiculoExternoId, e);
        }
    }
}
