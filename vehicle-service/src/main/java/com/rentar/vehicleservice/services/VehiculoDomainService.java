package com.rentar.vehicleservice.services;

import com.rentar.vehicleservice.entities.EstadoVehiculo;
import com.rentar.vehicleservice.entities.Vehiculo;
import com.rentar.vehicleservice.exceptions.PatenteDuplicadaException;
import com.rentar.vehicleservice.exceptions.VehiculoInvalidoException;
import com.rentar.vehicleservice.exceptions.VehiculoNoEncontradoException;
import com.rentar.vehicleservice.repositories.VehiculoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehiculoDomainService {

    private final VehiculoRepository repository;

    public VehiculoDomainService(VehiculoRepository repository) {
        this.repository = repository;
    }

    public Vehiculo crear(Vehiculo vehiculo) {
        validarCamposObligatorios(vehiculo);

        if (repository.existsByPatente(vehiculo.getPatente())) {
            throw new PatenteDuplicadaException(vehiculo.getPatente());
        }

        vehiculo.setId(null);
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        vehiculo.setActivo(true);
        return repository.save(vehiculo);
    }

    public Vehiculo modificar(Long id, Vehiculo datos) {
        Vehiculo existente = consultar(id);
        validarCamposObligatorios(datos);

        existente.setMarca(datos.getMarca());
        existente.setModelo(datos.getModelo());
        existente.setAnio(datos.getAnio());
        existente.setColor(datos.getColor());
        existente.setTipoVehiculo(datos.getTipoVehiculo());
        existente.setPrecioDiario(datos.getPrecioDiario());
        return repository.save(existente);
    }

    public void bajaLogica(Long id) {
        Vehiculo v = consultar(id);
        v.setActivo(false);
        repository.save(v);
    }

    public Vehiculo consultar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new VehiculoNoEncontradoException(id));
    }

    public List<Vehiculo> listar(Boolean activo) {
        if (activo == null) {
            return repository.findAll();
        }
        return repository.findByActivo(activo);
    }

    public void actualizarEstado(Long id, EstadoVehiculo nuevoEstado) {
        Vehiculo v = consultar(id);
        v.setEstado(nuevoEstado);
        repository.save(v);
    }

    public List<Vehiculo> consultarDisponibilidad(
            String tipoVehiculo, String marca, String modelo,
            Double precioMin, Double precioMax, List<Long> idsOcupados) {

        return repository.findByActivo(true).stream()
                .filter(v -> tipoVehiculo == null || v.getTipoVehiculo().name().equals(tipoVehiculo))
                .filter(v -> marca == null || v.getMarca().equalsIgnoreCase(marca))
                .filter(v -> modelo == null || v.getModelo().equalsIgnoreCase(modelo))
                .filter(v -> precioMin == null || v.getPrecioDiario() >= precioMin)
                .filter(v -> precioMax == null || v.getPrecioDiario() <= precioMax)
                .filter(v -> idsOcupados == null || !idsOcupados.contains(v.getId()))
                .toList();
    }

    private void validarCamposObligatorios(Vehiculo v) {
        if (v.getPatente() == null || v.getPatente().isBlank()) {
            throw new VehiculoInvalidoException("La patente es obligatoria");
        }
        if (v.getMarca() == null || v.getMarca().isBlank()) {
            throw new VehiculoInvalidoException("La marca es obligatoria");
        }
        if (v.getModelo() == null || v.getModelo().isBlank()) {
            throw new VehiculoInvalidoException("El modelo es obligatorio");
        }
        if (v.getAnio() == null || v.getAnio().isBlank()) {
            throw new VehiculoInvalidoException("El año es obligatorio");
        }
        if (v.getTipoVehiculo() == null) {
            throw new VehiculoInvalidoException("El tipo de vehículo es obligatorio");
        }
        if (v.getPrecioDiario() == null || v.getPrecioDiario() <= 0) {
            throw new VehiculoInvalidoException("El precio diario debe ser mayor a 0");
        }
    }
}