package com.rentar.registrotitularidad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Consume vehiculos.externos.registrados, obtiene el documento del cliente (Customer Service, gRPC),
 * lo compara con el titular de la patente en el registro y publica vehiculos.externos.validacion.titularidad
 * con correlation_id = vehiculoExternoId.
 */
@Component
public class TitularidadListener {

    private static final Logger log = LoggerFactory.getLogger(TitularidadListener.class);
    private static final String EXCHANGE = "rentar.eventos";
    private static final String EVENTO_RESULTADO = "vehiculos.externos.validacion.titularidad";
    static final String VALIDADO = "VALIDADO";
    static final String RECHAZADO = "RECHAZADO";

    private final RegistroTitulares registro;
    private final ClientesClient clientes;
    private final RabbitTemplate rabbitTemplate;

    public TitularidadListener(RegistroTitulares registro, ClientesClient clientes, RabbitTemplate rabbitTemplate) {
        this.registro = registro;
        this.clientes = clientes;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "titularidad.vehiculos.externos.registrados")
    public void onRegistrado(VehiculoExternoRegistrado evento) {
        validar(evento);

        Optional<String> documento = clientes.documentoDe(evento.clienteId());
        Optional<String> titular = registro.titularDe(evento.patente());
        boolean esTitular = documento.isPresent() && titular.isPresent() && titular.get().equals(documento.get());
        String resultado = esTitular ? VALIDADO : RECHAZADO;
        log.info("Solicitud {}: cliente {} (documento {}) patente {} -> {}",
                evento.vehiculoExternoId(), evento.clienteId(), documento.orElse("-"), evento.patente(), resultado);

        ValidacionTitularidad respuesta = new ValidacionTitularidad(
                evento.vehiculoExternoId(), evento.clienteId(), documento.orElse(null), evento.patente(), resultado);
        rabbitTemplate.convertAndSend(EXCHANGE, EVENTO_RESULTADO, respuesta, m -> {
            MessageProperties p = m.getMessageProperties();
            p.setMessageId(UUID.randomUUID().toString());
            p.setType(EVENTO_RESULTADO);
            p.setCorrelationId(String.valueOf(evento.vehiculoExternoId()));
            p.setTimestamp(new Date());
            return m;
        });
    }

    // Un mensaje mal formado no se arregla reintentando: va directo a la DLQ.
    private static void validar(VehiculoExternoRegistrado e) {
        if (e == null || e.vehiculoExternoId() == null || e.vehiculoExternoId() <= 0
                || e.clienteId() == null || e.clienteId() <= 0 || e.patente() == null || e.patente().isBlank()) {
            throw new AmqpRejectAndDontRequeueException("Evento vehiculos.externos.registrados inválido: " + e);
        }
    }
}
