package com.rentar.registrotitularidad;

import io.grpc.Status;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TitularidadListenerTest {

    @Mock
    private ClientesClient clientes;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private TitularidadListener listener;

    @BeforeEach
    void setUp() {
        RegistroTitulares registro = new RegistroTitulares(
                new ByteArrayResource("AB123CD;30111222\n".getBytes(StandardCharsets.UTF_8)));
        registro.cargar();
        listener = new TitularidadListener(registro, clientes, rabbitTemplate);
    }

    private ValidacionTitularidad publicado() {
        ArgumentCaptor<Object> evento = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("rentar.eventos"), eq("vehiculos.externos.validacion.titularidad"),
                evento.capture(), any(MessagePostProcessor.class));
        return (ValidacionTitularidad) evento.getValue();
    }

    @Test
    void validaCuandoElClienteEsElTitular() {
        when(clientes.documentoDe(89L)).thenReturn(Optional.of("30111222"));

        listener.onRegistrado(new VehiculoExternoRegistrado(50L, 89L, "AB123CD"));

        assertEquals(new ValidacionTitularidad(50L, 89L, "30111222", "AB123CD", "VALIDADO"), publicado());
    }

    @Test
    void rechazaCuandoElTitularEsOtraPersona() {
        when(clientes.documentoDe(89L)).thenReturn(Optional.of("40888999"));

        listener.onRegistrado(new VehiculoExternoRegistrado(50L, 89L, "AB123CD"));

        assertEquals("RECHAZADO", publicado().resultado());
    }

    @Test
    void rechazaCuandoLaPatenteNoFiguraEnElRegistro() {
        when(clientes.documentoDe(89L)).thenReturn(Optional.of("30111222"));

        listener.onRegistrado(new VehiculoExternoRegistrado(50L, 89L, "ZZ999ZZ"));

        assertEquals("RECHAZADO", publicado().resultado());
    }

    @Test
    void rechazaCuandoElClienteNoExiste() {
        when(clientes.documentoDe(89L)).thenReturn(Optional.empty());

        listener.onRegistrado(new VehiculoExternoRegistrado(50L, 89L, "AB123CD"));

        ValidacionTitularidad r = publicado();
        assertEquals("RECHAZADO", r.resultado());
        assertEquals(null, r.documento());
    }

    @Test
    void siElCustomerServiceFallaPropagaElErrorParaReintentar() {
        when(clientes.documentoDe(89L)).thenThrow(Status.UNAVAILABLE.asRuntimeException());

        assertThrows(RuntimeException.class,
                () -> listener.onRegistrado(new VehiculoExternoRegistrado(50L, 89L, "AB123CD")));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
    }

    @Test
    void unEventoMalFormadoVaDirectoALaDlq() {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> listener.onRegistrado(new VehiculoExternoRegistrado(50L, null, "AB123CD")));
    }
}
