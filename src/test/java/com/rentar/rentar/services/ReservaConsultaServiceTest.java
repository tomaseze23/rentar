package com.rentar.rentar.services;

import com.rentar.rentar.dtos.HistorialAlquilerResponse;
import com.rentar.rentar.dtos.ReservaConsultaResponse;
import com.rentar.rentar.dtos.ReservaFiltro;
import com.rentar.rentar.entities.*;
import com.rentar.rentar.repositories.ClienteRepository;
import com.rentar.rentar.repositories.ReservaRepository;
import com.rentar.rentar.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaConsultaServiceTest {
    @Mock ReservaRepository reservas;
    @Mock ClienteRepository clientes;
    @Mock UsuarioRepository usuarios;
    private ReservaConsultaService servicio;
    private Usuario usuario;
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        servicio = new ReservaConsultaService(reservas, clientes, usuarios);
        usuario = new Usuario("cliente@ejemplo.com", "test", "CLIENTE");
        usuario.setId(7L);
        cliente = new Cliente();
        cliente.setId(12L);
        cliente.setNombre("Ana");
        cliente.setApellido("Pérez");
        cliente.setActivo(true);
    }

    private Authentication sesion() {
        return new UsernamePasswordAuthenticationToken(usuario.getEmail(), "N/A",
                AuthorityUtils.createAuthorityList("ROLE_" + usuario.getRol()));
    }

    private void usuarioDisponible() {
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
    }

    private Reserva reserva(EstadoReserva estado, LocalDateTime inicio, LocalDateTime fin) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setId(99L);
        vehiculo.setMarca("Ford");
        vehiculo.setModelo("Focus");
        vehiculo.setPatente("ABC123");
        Reserva reserva = new Reserva();
        reserva.setId(100L);
        reserva.setCliente(cliente);
        reserva.setVehiculo(vehiculo);
        reserva.setFechaInicio(inicio);
        reserva.setFechaFin(fin);
        reserva.setPrecioDiario(new BigDecimal("15000.00"));
        reserva.setImporteTotal(new BigDecimal("30000.00"));
        reserva.setEstado(estado);
        return reserva;
    }

    @Test
    void noPermiteConsultasSinSesion() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> servicio.consultar(null, null));
        verifyNoInteractions(reservas);
    }

    @Test
    void noPermiteClienteConsultarOtroCliente() {
        usuarioDisponible();
        when(clientes.findByUsuarioId(7L)).thenReturn(Optional.of(cliente));
        ReservaFiltro filtro = new ReservaFiltro();
        filtro.setClienteId(999L);
        assertThrows(AccessDeniedException.class, () -> servicio.consultar(filtro, sesion()));
        verifyNoInteractions(reservas);
    }

    @Test
    void noPermiteUsuarioInactivo() {
        usuario.setActivo(false);
        usuarioDisponible();
        assertThrows(AccessDeniedException.class, () -> servicio.consultar(null, sesion()));
        verifyNoInteractions(reservas);
    }

    @Test
    void permiteConsultarReservasPropiasYSoloDevuelveLosCamposPermitidos() {
        usuarioDisponible();
        when(clientes.findByUsuarioId(7L)).thenReturn(Optional.of(cliente));
        Reserva reserva = reserva(EstadoReserva.CONFIRMADA,
                LocalDateTime.of(2026, 10, 1, 10, 0), LocalDateTime.of(2026, 10, 3, 10, 0));
        when(reservas.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(reserva));

        List<ReservaConsultaResponse> resultado = servicio.consultar(null, sesion());

        assertEquals(1, resultado.size());
        assertEquals("Ana Pérez", resultado.get(0).getCliente());
        assertEquals("ABC123", resultado.get(0).getPatente());
        assertEquals("15000.00", resultado.get(0).getPrecioDiario());
        assertEquals("CONFIRMADA", resultado.get(0).getEstado());
        verify(reservas).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void administradorPuedeConsultarSinFiltroDeCliente() {
        usuario.setRol("ADMINISTRADOR");
        usuarioDisponible();
        when(reservas.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());
        assertTrue(servicio.consultar(new ReservaFiltro(), sesion()).isEmpty());
        verifyNoInteractions(clientes);
    }

    @Test
    void rechazaRangoInvertido() {
        usuario.setRol("ADMINISTRADOR");
        usuarioDisponible();
        ReservaFiltro filtro = new ReservaFiltro();
        filtro.setFechaDesde("2026-11-02T10:00");
        filtro.setFechaHasta("2026-11-01T10:00");
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(filtro, sesion()));
        verifyNoInteractions(reservas);
    }

    @Test
    void rechazaFechaMalFormateada() {
        usuario.setRol("ADMINISTRADOR");
        usuarioDisponible();
        ReservaFiltro filtro = new ReservaFiltro();
        filtro.setFechaDesde("mañana");
        assertThrows(IllegalArgumentException.class, () -> servicio.consultar(filtro, sesion()));
    }

    @Test
    void historialMuestraFinalizadaPorFechaSinMutarEstadoPersistido() {
        usuarioDisponible();
        when(clientes.findByUsuarioId(7L)).thenReturn(Optional.of(cliente));
        LocalDateTime inicio = LocalDateTime.now().minusDays(5);
        Reserva completada = reserva(EstadoReserva.CONFIRMADA, inicio, inicio.plusHours(25));
        when(reservas.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(completada));

        List<HistorialAlquilerResponse> resultado = servicio.historial(sesion());

        assertEquals("FINALIZADA", resultado.get(0).getEstado());
        assertEquals(2L, resultado.get(0).getCantidadDias());
        assertEquals("30000.00", resultado.get(0).getImporteTotal());
        assertEquals(EstadoReserva.CONFIRMADA, completada.getEstado());
        verify(reservas, never()).save(any());
    }

    @Test
    void historialIncluyeCanceladasAunqueFechaSeaFutura() {
        usuarioDisponible();
        when(clientes.findByUsuarioId(7L)).thenReturn(Optional.of(cliente));
        LocalDateTime inicio = LocalDateTime.now().plusDays(10);
        Reserva cancelada = reserva(EstadoReserva.CANCELADA, inicio, inicio.plusHours(2));
        when(reservas.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(cancelada));
        HistorialAlquilerResponse resultado = servicio.historial(sesion()).get(0);
        assertEquals("CANCELADA", resultado.getEstado());
        assertEquals(1L, resultado.getCantidadDias());
    }

    @Test
    void administradorNoTieneAccesoAlHistorialPrivadoDeCliente() {
        usuario.setRol("ADMINISTRADOR");
        usuarioDisponible();
        assertThrows(AccessDeniedException.class, () -> servicio.historial(sesion()));
        verifyNoInteractions(reservas);
    }
}
