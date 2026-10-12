package com.rentar.vehicleservice.services;

import com.rentar.vehicleservice.entities.EstadoVehiculo;
import com.rentar.vehicleservice.entities.TipoVehiculo;
import com.rentar.vehicleservice.entities.Vehiculo;
import com.rentar.vehicleservice.exceptions.PatenteDuplicadaException;
import com.rentar.vehicleservice.exceptions.VehiculoInvalidoException;
import com.rentar.vehicleservice.exceptions.VehiculoNoEncontradoException;
import com.rentar.vehicleservice.exceptions.VehiculoYaActivoException;
import com.rentar.vehicleservice.repositories.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiculoDomainServiceTest {

    @Mock
    private VehiculoRepository repository;

    @InjectMocks
    private VehiculoDomainService domainService;

    private Vehiculo vehiculoEjemplo;

    @BeforeEach
    void setUp() {
        vehiculoEjemplo = new Vehiculo();
        vehiculoEjemplo.setId(1L);
        vehiculoEjemplo.setPatente("AB123CD");
        vehiculoEjemplo.setMarca("Toyota");
        vehiculoEjemplo.setModelo("Corolla");
        vehiculoEjemplo.setAnio("2022");
        vehiculoEjemplo.setTipoVehiculo(TipoVehiculo.SEDAN);
        vehiculoEjemplo.setPrecioDiario(45000.0);
        vehiculoEjemplo.setEstado(EstadoVehiculo.DISPONIBLE);
        vehiculoEjemplo.setActivo(true);
    }

    private Vehiculo nuevoValido() {
        Vehiculo v = new Vehiculo();
        v.setPatente("AB123CD");
        v.setMarca("Toyota");
        v.setModelo("Corolla");
        v.setAnio("2022");
        v.setTipoVehiculo(TipoVehiculo.SEDAN);
        v.setPrecioDiario(45000.0);
        return v;
    }

    @Test
    void crear_vehiculoValido_loGuardaComoDisponibleYActivo() {
        when(repository.existsByPatente("AB123CD")).thenReturn(false);
        when(repository.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehiculo resultado = domainService.crear(nuevoValido());

        assertEquals(EstadoVehiculo.DISPONIBLE, resultado.getEstado());
        assertTrue(resultado.getActivo());
        verify(repository).save(any(Vehiculo.class));
    }

    @Test
    void crear_patenteDuplicada_tiraExcepcion() {
        when(repository.existsByPatente("AB123CD")).thenReturn(true);

        assertThrows(PatenteDuplicadaException.class, () -> domainService.crear(nuevoValido()));
        verify(repository, never()).save(any());
    }

    @Test
    void crear_patenteConEspaciosYMinusculas_laNormaliza() {
        Vehiculo nuevo = nuevoValido();
        nuevo.setPatente(" ab 123 cd ");
        nuevo.setMarca(" Toyota ");

        when(repository.existsByPatente("AB123CD")).thenReturn(false);
        when(repository.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehiculo resultado = domainService.crear(nuevo);

        assertEquals("AB123CD", resultado.getPatente());
        assertEquals("Toyota", resultado.getMarca());
    }

    @Test
    void crear_sinMarca_tiraExcepcion() {
        Vehiculo nuevo = nuevoValido();
        nuevo.setMarca(null);

        assertThrows(VehiculoInvalidoException.class, () -> domainService.crear(nuevo));
    }

    @Test
    void crear_anioNoNumerico_tiraExcepcion() {
        Vehiculo nuevo = nuevoValido();
        nuevo.setAnio("veintiuno");

        assertThrows(VehiculoInvalidoException.class, () -> domainService.crear(nuevo));
    }

    @Test
    void crear_anioFueraDeRango_tiraExcepcion() {
        Vehiculo nuevo = nuevoValido();
        nuevo.setAnio("1850");

        assertThrows(VehiculoInvalidoException.class, () -> domainService.crear(nuevo));
    }

    @Test
    void crear_precioDiarioCero_tiraExcepcion() {
        Vehiculo nuevo = nuevoValido();
        nuevo.setPrecioDiario(0.0);

        assertThrows(VehiculoInvalidoException.class, () -> domainService.crear(nuevo));
    }

    @Test
    void consultar_idExistente_devuelveVehiculo() {
        when(repository.findById(1L)).thenReturn(Optional.of(vehiculoEjemplo));

        Vehiculo resultado = domainService.consultar(1L);

        assertEquals("AB123CD", resultado.getPatente());
    }

    @Test
    void consultar_idInexistente_tiraExcepcion() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(VehiculoNoEncontradoException.class, () -> domainService.consultar(999L));
    }

    @Test
    void bajaLogica_marcaInactivo() {
        when(repository.findById(1L)).thenReturn(Optional.of(vehiculoEjemplo));
        when(repository.save(any(Vehiculo.class))).thenReturn(vehiculoEjemplo);

        domainService.bajaLogica(1L);

        assertFalse(vehiculoEjemplo.getActivo());
        verify(repository).save(vehiculoEjemplo);
    }

    @Test
    void reactivar_vehiculoInactivo_loActiva() {
        vehiculoEjemplo.setActivo(false);
        when(repository.findById(1L)).thenReturn(Optional.of(vehiculoEjemplo));
        when(repository.save(any(Vehiculo.class))).thenReturn(vehiculoEjemplo);

        Vehiculo resultado = domainService.reactivar(1L);

        assertTrue(resultado.getActivo());
    }

    @Test
    void reactivar_vehiculoYaActivo_tiraExcepcion() {
        vehiculoEjemplo.setActivo(true);
        when(repository.findById(1L)).thenReturn(Optional.of(vehiculoEjemplo));

        assertThrows(VehiculoYaActivoException.class, () -> domainService.reactivar(1L));
    }

    @Test
    void consultarDisponibilidadSimple_precioMinMayorQuePrecioMax_tiraExcepcion() {
        assertThrows(VehiculoInvalidoException.class, () ->
                domainService.consultarDisponibilidadSimple(null, null, null, 100000.0, 50000.0, List.of()));
    }

    @Test
    void consultarDisponibilidadSimple_marcaParcial_encuentraCoincidencia() {
        when(repository.findByActivo(true)).thenReturn(List.of(vehiculoEjemplo));

        List<Vehiculo> resultado = domainService.consultarDisponibilidadSimple(
                null, "Toy", null, null, null, List.of());

        assertEquals(1, resultado.size());
    }

    @Test
    void consultarDisponibilidadSimple_excluyeIdsOcupados() {
        when(repository.findByActivo(true)).thenReturn(List.of(vehiculoEjemplo));

        List<Vehiculo> resultado = domainService.consultarDisponibilidadSimple(
                null, null, null, null, null, List.of(1L));

        assertTrue(resultado.isEmpty());
    }
}
