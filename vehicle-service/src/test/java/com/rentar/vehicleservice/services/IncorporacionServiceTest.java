package com.rentar.vehicleservice.services;

import com.rentar.vehicleservice.entities.EstadoSolicitudIncorporacion;
import com.rentar.vehicleservice.entities.EstadoVehiculo;
import com.rentar.vehicleservice.entities.ResultadoValidacion;
import com.rentar.vehicleservice.entities.SolicitudIncorporacion;
import com.rentar.vehicleservice.entities.TipoVehiculo;
import com.rentar.vehicleservice.entities.Vehiculo;
import com.rentar.vehicleservice.entities.VerificacionVehiculo;
import com.rentar.vehicleservice.exceptions.PatenteDuplicadaException;
import com.rentar.vehicleservice.exceptions.SolicitudEnCursoException;
import com.rentar.vehicleservice.exceptions.SolicitudNoEncontradaException;
import com.rentar.vehicleservice.exceptions.VehiculoInvalidoException;
import com.rentar.vehicleservice.messaging.ValidacionCompletada;
import com.rentar.vehicleservice.messaging.VehiculoExternoRegistrado;
import com.rentar.vehicleservice.repositories.SolicitudIncorporacionRepository;
import com.rentar.vehicleservice.repositories.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static com.rentar.vehicleservice.entities.ResultadoValidacion.RECHAZADO;
import static com.rentar.vehicleservice.entities.ResultadoValidacion.VALIDADO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncorporacionServiceTest {

    @Mock
    private SolicitudIncorporacionRepository solicitudes;

    @Mock
    private VehiculoRepository vehiculos;

    @Mock
    private ApplicationEventPublisher eventos;

    private IncorporacionService service;

    @BeforeEach
    void setUp() {
        // Validaciones reales del alta de vehículos; solo los repositorios y el publicador son mocks
        service = new IncorporacionService(solicitudes, vehiculos, new VehiculoDomainService(vehiculos, eventos), eventos);
    }

    private static SolicitudIncorporacion datosValidos() {
        SolicitudIncorporacion s = new SolicitudIncorporacion();
        s.setClienteId(1L);
        s.setPatente(" aa 100aa ");
        s.setMarca("Fiat");
        s.setModelo("Cronos");
        s.setAnio("2022");
        s.setTipoVehiculo(TipoVehiculo.SEDAN);
        s.setPrecioDiario(30000.0);
        return s;
    }

    private static SolicitudIncorporacion pendiente(long id) {
        SolicitudIncorporacion s = datosValidos();
        s.setId(id);
        s.setPatente("AA100AA");
        s.setEstado(EstadoSolicitudIncorporacion.PENDIENTE);
        s.setValidacionPatente(ResultadoValidacion.PENDIENTE);
        s.setValidacionTitularidad(ResultadoValidacion.PENDIENTE);
        s.setCompletadaPublicada(false);
        return s;
    }

    // --- incorporar ---

    @Test
    void incorporar_guardaPendienteNormalizadoYPublicaRegistrados() {
        when(solicitudes.save(any())).thenAnswer(inv -> {
            SolicitudIncorporacion s = inv.getArgument(0);
            s.setId(50L);
            return s;
        });

        SolicitudIncorporacion r = service.incorporar(datosValidos());

        assertEquals("AA100AA", r.getPatente());
        assertEquals(EstadoSolicitudIncorporacion.PENDIENTE, r.getEstado());
        assertEquals(ResultadoValidacion.PENDIENTE, r.getValidacionPatente());
        assertEquals(ResultadoValidacion.PENDIENTE, r.getValidacionTitularidad());
        assertNotNull(r.getFechaSolicitud());
        verify(eventos).publishEvent(new VehiculoExternoRegistrado(50L, 1L, "AA100AA"));
    }

    @Test
    void incorporar_patenteYaEsUnVehiculo_lanzaPatenteDuplicada() {
        when(vehiculos.existsByPatente("AA100AA")).thenReturn(true);

        assertThrows(PatenteDuplicadaException.class, () -> service.incorporar(datosValidos()));
        verify(solicitudes, never()).save(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void incorporar_patenteConSolicitudEnCurso_lanzaSolicitudEnCurso() {
        when(solicitudes.existsByPatenteAndEstado("AA100AA", EstadoSolicitudIncorporacion.PENDIENTE)).thenReturn(true);

        assertThrows(SolicitudEnCursoException.class, () -> service.incorporar(datosValidos()));
        verify(solicitudes, never()).save(any());
    }

    @Test
    void incorporar_datosInvalidos_lanzaVehiculoInvalido() {
        SolicitudIncorporacion sinCliente = datosValidos();
        sinCliente.setClienteId(0L);
        SolicitudIncorporacion sinPrecio = datosValidos();
        sinPrecio.setPrecioDiario(0.0);

        assertThrows(VehiculoInvalidoException.class, () -> service.incorporar(sinCliente));
        assertThrows(VehiculoInvalidoException.class, () -> service.incorporar(sinPrecio));
    }

    @Test
    void listar_clienteIdInvalido_lanzaVehiculoInvalido() {
        assertThrows(VehiculoInvalidoException.class, () -> service.listar(0L));
    }

    // --- correlación de validaciones ---

    @Test
    void ambasValidadas_publicaCompletadaUnaSolaVez() {
        SolicitudIncorporacion s = pendiente(50L);
        when(solicitudes.buscarParaActualizar(50L)).thenReturn(Optional.of(s));

        service.registrarValidacionTitularidad(50L, VALIDADO);
        verify(eventos, never()).publishEvent(any(ValidacionCompletada.class));

        service.registrarValidacionPatente(50L, VALIDADO);
        service.registrarValidacionPatente(50L, VALIDADO); // duplicado

        verify(eventos, times(1)).publishEvent(new ValidacionCompletada(50L));
        assertTrue(s.getCompletadaPublicada());
        assertEquals(EstadoSolicitudIncorporacion.PENDIENTE, s.getEstado());
    }

    @Test
    void unaRechazada_quedaRechazadaYNoSePublicaCompletada() {
        SolicitudIncorporacion s = pendiente(50L);
        when(solicitudes.buscarParaActualizar(50L)).thenReturn(Optional.of(s));

        service.registrarValidacionPatente(50L, RECHAZADO);
        service.registrarValidacionTitularidad(50L, VALIDADO); // llega después: se guarda, no cambia nada

        assertEquals(EstadoSolicitudIncorporacion.RECHAZADA, s.getEstado());
        assertEquals(VALIDADO, s.getValidacionTitularidad());
        verify(eventos, never()).publishEvent(any(ValidacionCompletada.class));
    }

    @Test
    void resultadoDuplicadoDistinto_seIgnoraElSegundo() {
        SolicitudIncorporacion s = pendiente(50L);
        when(solicitudes.buscarParaActualizar(50L)).thenReturn(Optional.of(s));

        service.registrarValidacionTitularidad(50L, VALIDADO);
        service.registrarValidacionTitularidad(50L, RECHAZADO);

        assertEquals(VALIDADO, s.getValidacionTitularidad());
        assertEquals(EstadoSolicitudIncorporacion.PENDIENTE, s.getEstado());
    }

    @Test
    void validacionDeSolicitudInexistente_lanzaSolicitudNoEncontrada() {
        when(solicitudes.buscarParaActualizar(99L)).thenReturn(Optional.empty());

        assertThrows(SolicitudNoEncontradaException.class, () -> service.registrarValidacionPatente(99L, VALIDADO));
    }

    // --- completar ---

    @Test
    void completar_creaElVehiculoDisponibleANombreDelClienteYQuedaIncorporada() {
        SolicitudIncorporacion s = pendiente(50L);
        s.setValidacionPatente(VALIDADO);
        s.setValidacionTitularidad(VALIDADO);
        s.setCompletadaPublicada(true);
        when(solicitudes.buscarParaActualizar(50L)).thenReturn(Optional.of(s));
        when(vehiculos.save(any())).thenAnswer(inv -> {
            Vehiculo v = inv.getArgument(0);
            v.setId(16L);
            return v;
        });

        service.completar(50L);

        ArgumentCaptor<Vehiculo> creado = ArgumentCaptor.forClass(Vehiculo.class);
        verify(vehiculos).save(creado.capture());
        Vehiculo v = creado.getValue();
        assertEquals("AA100AA", v.getPatente());
        assertEquals(EstadoVehiculo.DISPONIBLE, v.getEstado());
        assertTrue(v.getActivo());
        assertEquals(VerificacionVehiculo.VALIDADO, v.getVerificacion());
        assertEquals(1L, v.getPropietarioClienteId());
        assertEquals(EstadoSolicitudIncorporacion.INCORPORADA, s.getEstado());
        assertEquals(16L, s.getVehiculoId());
        // No se vuelve a pedir la verificación policial: la patente ya se validó en este flujo
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void completar_duplicado_noCreaOtroVehiculo() {
        SolicitudIncorporacion s = pendiente(50L);
        s.setEstado(EstadoSolicitudIncorporacion.INCORPORADA);
        s.setVehiculoId(16L);
        when(solicitudes.buscarParaActualizar(50L)).thenReturn(Optional.of(s));

        service.completar(50L);

        verify(vehiculos, never()).save(any());
    }

    @Test
    void completar_sinAmbasValidaciones_noHaceNada() {
        SolicitudIncorporacion s = pendiente(50L);
        s.setValidacionPatente(VALIDADO);
        when(solicitudes.buscarParaActualizar(50L)).thenReturn(Optional.of(s));

        service.completar(50L);

        verify(vehiculos, never()).save(any());
        assertEquals(EstadoSolicitudIncorporacion.PENDIENTE, s.getEstado());
    }

    @Test
    void completar_patenteDadaDeAltaMientrasSeValidaba_quedaRechazada() {
        SolicitudIncorporacion s = pendiente(50L);
        s.setValidacionPatente(VALIDADO);
        s.setValidacionTitularidad(VALIDADO);
        when(solicitudes.buscarParaActualizar(50L)).thenReturn(Optional.of(s));
        when(vehiculos.existsByPatente("AA100AA")).thenReturn(true);

        service.completar(50L);

        verify(vehiculos, never()).save(any());
        assertEquals(EstadoSolicitudIncorporacion.RECHAZADA, s.getEstado());
    }
}
