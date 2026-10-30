package com.rentar.registropolicial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ValidacionPatenteListenerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ValidacionPatenteListener listener;

    @BeforeEach
    void setUp() {
        RegistroPatentes registro = new RegistroPatentes(new ByteArrayResource("ROB001\n".getBytes(StandardCharsets.UTF_8)));
        registro.cargar();
        listener = new ValidacionPatenteListener(registro, rabbitTemplate);
    }

    private ValidacionPatente publicado() {
        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("rentar.eventos"), eq("vehiculos.externos.validacion.patente"),
                evento.capture(), any(MessagePostProcessor.class));
        return (ValidacionPatente) evento.getValue();
    }

    @Test
    void patenteNoDenunciada_validado() {
        listener.onRegistrado(new VehiculoExternoRegistrado(50L, 1L, "AA100AA"));

        assertEquals(new ValidacionPatente(50L, "AA100AA", "VALIDADO"), publicado());
    }

    @Test
    void patenteDenunciada_rechazado() {
        listener.onRegistrado(new VehiculoExternoRegistrado(51L, 1L, "rob 001"));

        assertEquals("RECHAZADO", publicado().resultado());
    }

    @Test
    void eventoMalFormado_vaALaDlq() {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.onRegistrado(new VehiculoExternoRegistrado(50L, 1L, " ")));
        verifyNoInteractions(rabbitTemplate);
    }
}
