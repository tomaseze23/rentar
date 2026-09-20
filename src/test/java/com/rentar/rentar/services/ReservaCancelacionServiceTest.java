package com.rentar.rentar.services;

import com.rentar.rentar.dtos.ReservaResponse;
import com.rentar.rentar.entities.Cliente;
import com.rentar.rentar.entities.EstadoReserva;
import com.rentar.rentar.entities.Reserva;
import com.rentar.rentar.entities.Vehiculo;
import com.rentar.rentar.exceptions.ResourceNotFoundException;
import com.rentar.rentar.repositories.ClienteRepository;
import com.rentar.rentar.repositories.ReservaRepository;
import com.rentar.rentar.repositories.VehiculoRepository;
import com.rentar.rentar.services.implementation.ReservaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de cancelación de reservas")
public class ReservaCancelacionServiceTest {

    private static final String EMAIL = "cliente@mail.com";

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private Cliente clienteAutenticado;

    @BeforeEach
    void setUp() {
        clienteAutenticado = new Cliente();
        clienteAutenticado.setId(1L);
    }

    /**
     * Arma una reserva CONFIRMADA del cliente autenticado con la fecha de inicio indicada.
     * Sirve para simular tanto una reserva futura (cancelable) como una ya iniciada.
     */
    private Reserva buildReservaConfirmada(LocalDateTime fechaInicio) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(2L);

        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setCliente(clienteAutenticado);
        reserva.setVehiculo(vehiculo);
        reserva.setFechaInicio(fechaInicio);
        reserva.setFechaFin(fechaInicio.plusDays(4));
        reserva.setPrecioDiario(new BigDecimal("15000"));
        reserva.setImporteTotal(new BigDecimal("60000"));
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reserva;
    }

    @Test
    @DisplayName("Debe cancelar una reserva futura y dejarla en estado CANCELADA")
    void testCancelarReserva_Exito() {
        Reserva reserva = buildReservaConfirmada(LocalDateTime.now().plusDays(5));

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(clienteRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.of(clienteAutenticado));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaResponse response = reservaService.cancelarReserva(1L, EMAIL);

        assertNotNull(response);
        assertEquals("CANCELADA", response.getEstado());
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la reserva no existe")
    void testCancelarReserva_NoEncontrada() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                reservaService.cancelarReserva(99L, EMAIL)
        );

        // No debe intentar guardar nada si la reserva no existe
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar FORBIDDEN si la reserva pertenece a otro cliente")
    void testCancelarReserva_DeOtroCliente() {
        Reserva reserva = buildReservaConfirmada(LocalDateTime.now().plusDays(5));
        Cliente otroCliente = new Cliente();
        otroCliente.setId(99L);

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(clienteRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.of(otroCliente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reservaService.cancelarReserva(1L, EMAIL)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar CONFLICT si la reserva ya se encuentra cancelada")
    void testCancelarReserva_YaCancelada() {
        Reserva reserva = buildReservaConfirmada(LocalDateTime.now().plusDays(5));
        reserva.setEstado(EstadoReserva.CANCELADA);

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(clienteRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.of(clienteAutenticado));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reservaService.cancelarReserva(1L, EMAIL)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar CONFLICT si el período de la reserva ya comenzó")
    void testCancelarReserva_PeriodoYaComenzado() {
        // Fecha de inicio en el pasado: el alquiler ya arrancó
        Reserva reserva = buildReservaConfirmada(LocalDateTime.now().minusHours(1));

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(clienteRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.of(clienteAutenticado));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reservaService.cancelarReserva(1L, EMAIL)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        // La reserva no debe cambiar de estado ni guardarse
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        verify(reservaRepository, never()).save(any());
    }
}
