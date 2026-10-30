package com.rental.rentalservice.services;

import com.rental.rentalservice.events.MantenimientoResultado;
import com.rental.rentalservice.events.ReservaConfirmada;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MantenimientoEventListener {

    private final MantenimientoService mantenimientoService;
    private final ReservaService reservaService;

    public MantenimientoEventListener(MantenimientoService mantenimientoService, ReservaService reservaService) {
        this.mantenimientoService = mantenimientoService;
        this.reservaService = reservaService;
    }

    @RabbitListener(queues = "mantenimiento.reserva.confirmada")
    public void procesarReservaConfirmada(ReservaConfirmada evento) {
        mantenimientoService.procesar(evento);
    }

    @RabbitListener(queues = "rental-service.mantenimiento.resultado")
    public void registrarResultado(MantenimientoResultado evento, Message mensaje) {
        String correlationId = mensaje.getMessageProperties().getCorrelationId();
        if (!String.valueOf(evento.reservaId()).equals(correlationId)) {
            throw new IllegalArgumentException("correlation_id no coincide con reservaId");
        }
        reservaService.registrarResultadoMantenimiento(evento);
    }
}
