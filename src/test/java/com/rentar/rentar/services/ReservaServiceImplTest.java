package com.rentar.rentar.services;


import com.rentar.rentar.dtos.ReservaRequest;
import com.rentar.rentar.dtos.ReservaResponse;
import com.rentar.rentar.entities.Cliente;
import com.rentar.rentar.entities.EstadoReserva;
import com.rentar.rentar.entities.Reserva;
import com.rentar.rentar.entities.Vehiculo;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    private static final String EMAIL = "cliente@mail.com";

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private Cliente clienteActivo;
    private Vehiculo vehiculoActivo;

    @BeforeEach
    void setUp() {
        clienteActivo = new Cliente();
        clienteActivo.setId(1L);
        clienteActivo.setActivo(true);

        vehiculoActivo = new Vehiculo();
        vehiculoActivo.setId(2L);
        vehiculoActivo.setActivo(true);
        vehiculoActivo.setPrecioDiario(2000.0);
    }

    @Test
    @DisplayName("Debe crear la reserva para el cliente autenticado y calcular el total (48hs = 2 días)")
    void testCrearReserva_CalculoYExito() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fin = inicio.plusHours(48); // 2 días
        ReservaRequest request = new ReservaRequest(2L, inicio, fin);

        when(clienteRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.of(clienteActivo));
        when(vehiculoRepository.findById(2L)).thenReturn(Optional.of(vehiculoActivo));
        when(reservaRepository.existsSolapamiento(eq(2L), any(), any(), eq(EstadoReserva.CONFIRMADA))).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> {
            Reserva r = invocation.getArgument(0);
            r.setId(10L);
            return r;
        });

        ReservaResponse response = reservaService.crearReserva(request, EMAIL);

        assertNotNull(response);
        assertEquals("CONFIRMADA", response.getEstado());
        assertEquals(1L, response.getClienteId());
        assertEquals(0, new BigDecimal("4000.00").compareTo(response.getImporteTotal())); // 2000 * 2 días
    }

    @Test
    @DisplayName("Debe cobrar 2 días si el alquiler excede las 24hs aunque sea por minutos")
    void testCrearReserva_FraccionDeDiaSeCobraCompleta() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fin = inicio.plusHours(24).plusMinutes(1);
        ReservaRequest request = new ReservaRequest(2L, inicio, fin);

        when(clienteRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.of(clienteActivo));
        when(vehiculoRepository.findById(2L)).thenReturn(Optional.of(vehiculoActivo));
        when(reservaRepository.existsSolapamiento(eq(2L), any(), any(), eq(EstadoReserva.CONFIRMADA))).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservaResponse response = reservaService.crearReserva(request, EMAIL);

        assertEquals(0, new BigDecimal("4000.00").compareTo(response.getImporteTotal())); // 2000 * 2 días
    }

    @Test
    @DisplayName("Debe lanzar BAD_REQUEST si fechaFin no es posterior a fechaInicio")
    void testCrearReserva_FechasInvertidas() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(5);
        LocalDateTime fin = LocalDateTime.now().plusDays(2);
        ReservaRequest request = new ReservaRequest(2L, inicio, fin);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reservaService.crearReserva(request, EMAIL)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe lanzar UNPROCESSABLE_ENTITY si el cliente no está activo")
    void testCrearReserva_ClienteInactivo() {
        clienteActivo.setActivo(false);
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fin = LocalDateTime.now().plusDays(3);
        ReservaRequest request = new ReservaRequest(2L, inicio, fin);

        when(clienteRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.of(clienteActivo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reservaService.crearReserva(request, EMAIL)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe lanzar NOT_FOUND si el usuario autenticado no tiene un cliente asociado")
    void testCrearReserva_UsuarioSinCliente() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        ReservaRequest request = new ReservaRequest(2L, inicio, inicio.plusDays(2));

        when(clienteRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reservaService.crearReserva(request, EMAIL)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar UNPROCESSABLE_ENTITY si el vehículo no está activo")
    void testCrearReserva_VehiculoInactivo() {
        vehiculoActivo.setActivo(false);
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        ReservaRequest request = new ReservaRequest(2L, inicio, inicio.plusDays(2));

        when(clienteRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.of(clienteActivo));
        when(vehiculoRepository.findById(2L)).thenReturn(Optional.of(vehiculoActivo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reservaService.crearReserva(request, EMAIL)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe lanzar CONFLICT si el vehículo ya tiene una reserva confirmada en el período")
    void testCrearReserva_Solapamiento() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        ReservaRequest request = new ReservaRequest(2L, inicio, inicio.plusDays(2));

        when(clienteRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.of(clienteActivo));
        when(vehiculoRepository.findById(2L)).thenReturn(Optional.of(vehiculoActivo));
        when(reservaRepository.existsSolapamiento(eq(2L), any(), any(), eq(EstadoReserva.CONFIRMADA))).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reservaService.crearReserva(request, EMAIL)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(reservaRepository, never()).save(any());
    }
}
