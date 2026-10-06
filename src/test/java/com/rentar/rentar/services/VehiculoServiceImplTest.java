package com.rentar.rentar.services;

import com.rentar.rentar.dtos.VehiculoRequest;
import com.rentar.rentar.dtos.VehiculoResponse;
import com.rentar.rentar.dtos.VehiculoUpdateRequest;
import com.rentar.rentar.entities.EstadoVehiculo;
import com.rentar.rentar.entities.TipoVehiculo;
import com.rentar.rentar.entities.Vehiculo;
import com.rentar.rentar.exceptions.DuplicateResourceException;
import com.rentar.rentar.exceptions.ResourceNotFoundException;
import com.rentar.rentar.repositories.VehiculoRepository;
import com.rentar.rentar.services.implementation.VehiculoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceImplTest {

    @Mock
    private VehiculoRepository repo;

    private VehiculoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VehiculoServiceImpl(repo);
    }

    private VehiculoRequest alta(String patente, String anio) {
        return new VehiculoRequest(patente, " Toyota ", "Corolla", anio, "", TipoVehiculo.SEDAN, 45000.0);
    }

    private VehiculoUpdateRequest modificacion(String patente) {
        return new VehiculoUpdateRequest(patente, "Toyota", "Yaris", "2023", "Rojo", TipoVehiculo.HATCHBACK, 30000.0);
    }

    private Vehiculo existente(boolean activo) {
        Vehiculo v = new Vehiculo(1L, "AB123CD", "Toyota", "Corolla", "2022", "Gris",
                TipoVehiculo.SEDAN, 45000.0, EstadoVehiculo.DISPONIBLE, activo);
        return v;
    }

    @Test
    void crear_quedaActivoDisponibleYNormalizaLosDatos() {
        when(repo.existsByPatente("AB123CD")).thenReturn(false);
        when(repo.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        VehiculoResponse creado = service.crear(alta("ab 123 cd", "2022"));

        assertThat(creado.getPatente()).isEqualTo("AB123CD");
        assertThat(creado.getMarca()).isEqualTo("Toyota");
        assertThat(creado.getColor()).isNull();
        assertThat(creado.getEstado()).isEqualTo(EstadoVehiculo.DISPONIBLE);
        assertThat(creado.getActivo()).isTrue();
    }

    @Test
    void crear_conPatenteDuplicadaLanzaDuplicateResource() {
        when(repo.existsByPatente("AB123CD")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(alta("AB123CD", "2022")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("AB123CD");
        verify(repo, never()).save(any());
    }

    @Test
    void crear_conAnioFueraDeRangoEsRechazado() {
        when(repo.existsByPatente(any())).thenReturn(false);
        String anioFuturo = String.valueOf(Year.now().getValue() + 2);

        assertThatThrownBy(() -> service.crear(alta("AB123CD", "1850")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.crear(alta("AB123CD", anioFuturo)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void modificar_actualizaDatosPeroNoLaPatente() {
        when(repo.findById(1L)).thenReturn(Optional.of(existente(true)));
        when(repo.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        VehiculoResponse actualizado = service.modificar(1L, modificacion(null));

        assertThat(actualizado.getPatente()).isEqualTo("AB123CD");
        assertThat(actualizado.getModelo()).isEqualTo("Yaris");
        assertThat(actualizado.getTipoVehiculo()).isEqualTo(TipoVehiculo.HATCHBACK);
        assertThat(actualizado.getPrecioDiario()).isEqualTo(30000.0);
    }

    @Test
    void modificar_aceptaLaMismaPatenteYRechazaUnaDistinta() {
        when(repo.findById(1L)).thenReturn(Optional.of(existente(true)));
        when(repo.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        service.modificar(1L, modificacion("ab123cd"));

        assertThatThrownBy(() -> service.modificar(1L, modificacion("ZZ999ZZ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("patente no puede modificarse");
    }

    @Test
    void modificar_vehiculoInexistenteLanzaNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modificar(99L, modificacion(null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bajaLogica_marcaInactivoSinBorrar() {
        when(repo.findById(1L)).thenReturn(Optional.of(existente(true)));

        service.bajaLogica(1L);

        ArgumentCaptor<Vehiculo> guardado = ArgumentCaptor.forClass(Vehiculo.class);
        verify(repo).save(guardado.capture());
        assertThat(guardado.getValue().getActivo()).isFalse();
        verify(repo, never()).deleteById(any());
    }

        @Test
    void reactivar_vuelveAActivarUnVehiculoDadoDeBaja() {
        when(repo.findById(1L)).thenReturn(Optional.of(existente(false)));
        when(repo.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.reactivar(1L).getActivo()).isTrue();
    }

    @Test
    void reactivar_unVehiculoActivoEsConflicto() {
        when(repo.findById(1L)).thenReturn(Optional.of(existente(true)));

        assertThatThrownBy(() -> service.reactivar(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ya se encuentra activo");
        verify(repo, never()).save(any());
    }
}