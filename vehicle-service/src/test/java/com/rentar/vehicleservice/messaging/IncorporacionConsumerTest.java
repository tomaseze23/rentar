package com.rentar.vehicleservice.messaging;

import com.rentar.vehicleservice.entities.ResultadoValidacion;
import com.rentar.vehicleservice.exceptions.SolicitudNoEncontradaException;
import com.rentar.vehicleservice.services.IncorporacionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncorporacionConsumerTest {

    @Mock
    private IncorporacionService service;

    @InjectMocks
    private IncorporacionConsumer consumer;

    @Test
    void registraCadaValidacionYCompleta() {
        consumer.onValidacionPatente(new ValidacionPatente(50L, "AA100AA", "VALIDADO"));
        consumer.onValidacionTitularidad(new ValidacionTitularidad(50L, 1L, "30111222", "AA100AA", "RECHAZADO"));
        consumer.onCompletada(new ValidacionCompletada(50L));

        verify(service).registrarValidacionPatente(50L, ResultadoValidacion.VALIDADO);
        verify(service).registrarValidacionTitularidad(50L, ResultadoValidacion.RECHAZADO);
        verify(service).completar(50L);
    }

    @Test
    void resultadoInvalidoOSinId_vaALaDlq() {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumer.onValidacionPatente(new ValidacionPatente(50L, "AA100AA", "ROBADO")));
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumer.onValidacionTitularidad(new ValidacionTitularidad(null, 1L, null, "AA100AA", "VALIDADO")));
        verifyNoInteractions(service);
    }

    @Test
    void solicitudInexistente_vaALaDlq() {
        doThrow(new SolicitudNoEncontradaException(99L)).when(service).completar(99L);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer.onCompletada(new ValidacionCompletada(99L)));
    }
}
