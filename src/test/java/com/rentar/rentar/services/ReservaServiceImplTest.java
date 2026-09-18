package com.rentar.rentar.services;


import com.rentar.rentar.dtos.ReservaRequest;
import com.rentar.rentar.dtos.ReservaResponse;
import com.rentar.rentar.entities.Cliente;
import com.rentar.rentar.entities.EstadoVehiculo;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

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
    @DisplayName("Debe crear la reserva y calcular el total correctamente (48hs = 2 días)")
    void testCrearReserva_CalculoYExito() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fin = inicio.plusHours(48); // 2 días
        ReservaRequest request = new ReservaRequest(1L, 2L, inicio, fin);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));
        when(vehiculoRepository.findById(2L)).thenReturn(Optional.of(vehiculoActivo));
        when(reservaRepository.existsSolapamiento(eq(2L), any(), any(), eq(EstadoVehiculo.RESERVADO))).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> {
            Reserva r = invocation.getArgument(0);
            r.setId(10L);
            return r;
        });

        ReservaResponse response = reservaService.crearReserva(request);

        assertNotNull(response);
        assertEquals("RESERVADO", response.getEstado());
        assertEquals(0, new BigDecimal("40000.00").compareTo(response.getImporteTotal())); // 20000 * 2 días
    }

    @Test
    @DisplayName("Debe lanzar BAD_REQUEST si fechaFin no es posterior a fechaInicio")
    void testCrearReserva_FechasInvertidas() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(5);
        LocalDateTime fin = LocalDateTime.now().plusDays(2);
        ReservaRequest request = new ReservaRequest(1L, 2L, inicio, fin);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reservaService.crearReserva(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe lanzar UNPROCESSABLE_ENTITY si el cliente no está activo")
    void testCrearReserva_ClienteInactivo() {
        clienteActivo.setActivo(false);
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fin = LocalDateTime.now().plusDays(3);
        ReservaRequest request = new ReservaRequest(1L, 2L, inicio, fin);

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteActivo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                reservaService.crearReserva(request)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }
}