package com.rentar.registropolicial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * Consume vehiculos.verificacion.solicitada, consulta el registro y publica
 * vehiculos.verificacion.resultado copiando el correlation_id (= vehiculoId).
 */
@Component
public class VerificacionListener {

    private static final Logger log = LoggerFactory.getLogger(VerificacionListener.class);
    private static final String EXCHANGE = "rentar.eventos";
    private static final String EVENTO_RESULTADO = "vehiculos.verificacion.resultado";

    private final RegistroPatentes registro;
    private final RabbitTemplate rabbitTemplate;

    public VerificacionListener(RegistroPatentes registro, RabbitTemplate rabbitTemplate) {
        this.registro = registro;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "registro-policial.vehiculos.verificacion.solicitada")
    public void onSolicitada(VerificacionSolicitada evento) {
        String resultado = registro.estaDenunciada(evento.patente()) ? "ROBADO" : "VALIDADO";
        log.info("Vehículo {} patente {} -> {}", evento.vehiculoId(), evento.patente(), resultado);

        VerificacionResultado respuesta = new VerificacionResultado(evento.vehiculoId(), evento.patente(), resultado);
        rabbitTemplate.convertAndSend(EXCHANGE, EVENTO_RESULTADO, respuesta, m -> {
            MessageProperties p = m.getMessageProperties();
            p.setMessageId(UUID.randomUUID().toString());
            p.setType(EVENTO_RESULTADO);
            p.setCorrelationId(String.valueOf(evento.vehiculoId()));
            p.setTimestamp(new Date());
            return m;
        });
    }
}
