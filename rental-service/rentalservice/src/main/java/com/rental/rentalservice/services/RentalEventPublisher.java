package com.rental.rentalservice.services;

import com.rental.rentalservice.events.MantenimientoResultado;
import com.rental.rentalservice.events.ReservaConfirmada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Date;
import java.util.UUID;

@Component
public class RentalEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RentalEventPublisher.class);
    private static final String EXCHANGE = "rentar.eventos";

    private final RabbitTemplate rabbitTemplate;

    public RentalEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicarReservaConfirmada(ReservaConfirmada evento) {
        publicar("reserva.confirmada", String.valueOf(evento.reservaId()), evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicarResultadoMantenimiento(MantenimientoResultado evento) {
        publicar("mantenimiento.resultado", String.valueOf(evento.reservaId()), evento);
    }

    private void publicar(String tipo, String correlationId, Object evento) {
        MessagePostProcessor propiedades = (Message mensaje) -> {
            mensaje.getMessageProperties().setMessageId(UUID.randomUUID().toString());
            mensaje.getMessageProperties().setType(tipo);
            mensaje.getMessageProperties().setCorrelationId(correlationId);
            mensaje.getMessageProperties().setTimestamp(new Date());
            return mensaje;
        };
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, tipo, evento, propiedades);
        } catch (AmqpException exception) {
            logger.error("No se pudo publicar el evento {} para correlation_id={}", tipo, correlationId, exception);
        }
    }
}
