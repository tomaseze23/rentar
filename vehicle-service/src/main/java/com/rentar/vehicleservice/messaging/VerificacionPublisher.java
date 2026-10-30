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

@Component
public class VerificacionPublisher {

    private static final Logger log = LoggerFactory.getLogger(VerificacionPublisher.class);
    private static final String EXCHANGE = "rentar.eventos";
    private static final String EVENTO = "vehiculos.verificacion.solicitada";

    private final RabbitTemplate rabbitTemplate;

    public VerificacionPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alCrearseUnVehiculo(VehiculoCreado creado) {
        VerificacionSolicitada evento = new VerificacionSolicitada(creado.vehiculoId(), creado.patente());
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, EVENTO, evento, m -> {
                MessageProperties p = m.getMessageProperties();
                p.setMessageId(UUID.randomUUID().toString());
                p.setType(EVENTO);
                p.setCorrelationId(String.valueOf(evento.vehiculoId()));
                p.setTimestamp(new Date());
                return m;
            });
            log.info("Publicado {} para vehículo {}", EVENTO, evento.vehiculoId());
        } catch (AmqpException e) {
            log.error("No se pudo publicar {} para vehículo {}", EVENTO, evento.vehiculoId(), e);
        }
    }
}