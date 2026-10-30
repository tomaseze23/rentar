package com.rentar.vehicleservice.messaging;

import com.rentar.vehicleservice.entities.VerificacionVehiculo;
import com.rentar.vehicleservice.services.VehiculoDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class VerificacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(VerificacionConsumer.class);

    private final VehiculoDomainService domainService;

    public VerificacionConsumer(VehiculoDomainService domainService) {
        this.domainService = domainService;
    }

    @RabbitListener(queues = "vehicle-service.vehiculos.verificacion.resultado")
    public void onResultado(VerificacionResultado evento) {
        VerificacionVehiculo resultado = parsear(evento);
        log.info("Resultado de verificación del vehículo {}: {}", evento.vehiculoId(), resultado);
        domainService.aplicarVerificacion(evento.vehiculoId(), resultado);
    }

    private VerificacionVehiculo parsear(VerificacionResultado evento) {
        try {
            VerificacionVehiculo r = VerificacionVehiculo.valueOf(evento.resultado());
            if (r == VerificacionVehiculo.PENDIENTE) {
                throw new IllegalArgumentException("PENDIENTE no es un resultado");
            }
            return r;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AmqpRejectAndDontRequeueException("Resultado inválido: " + evento.resultado(), e);
        }
    }
}