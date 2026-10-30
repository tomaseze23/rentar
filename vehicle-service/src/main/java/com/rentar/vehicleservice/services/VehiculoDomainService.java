package com.rentar.vehicleservice.services;

import com.rentar.vehicleservice.entities.EstadoVehiculo;
import com.rentar.vehicleservice.entities.TipoVehiculo;
import com.rentar.vehicleservice.entities.Vehiculo;
import com.rentar.vehicleservice.exceptions.PatenteDuplicadaException;
import com.rentar.vehicleservice.exceptions.VehiculoInvalidoException;
import com.rentar.vehicleservice.exceptions.VehiculoNoEncontradoException;
import com.rentar.vehicleservice.exceptions.VehiculoYaActivoException;
import com.rentar.vehicleservice.repositories.VehiculoRepository;
import org.springframework.stereotype.Service;
import com.rentar.vehicleservice.entities.VerificacionVehiculo;
import com.rentar.vehicleservice.messaging.VehiculoCreado;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehiculoDomainService {

    private final VehiculoRepository repository;
    private final ApplicationEventPublisher eventos;

    public VehiculoDomainService(VehiculoRepository repository, ApplicationEventPublisher eventos) {
        this.repository = repository;
        this.eventos = eventos;
    }

    public Vehiculo crear(Vehiculo vehiculo) {
        normalizarCampos(vehiculo);
        validarCamposObligatorios(vehiculo);

        if (repository.existsByPatente(vehiculo.getPatente())) {
            throw new PatenteDuplicadaException(vehiculo.getPatente());
        }

        vehiculo.setId(null);
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        vehiculo.setActivo(true);
        vehiculo.setVerificacion(VerificacionVehiculo.PENDIENTE);
        Vehiculo guardado = repository.save(vehiculo);

        eventos.publishEvent(new VehiculoCreado(guardado.getId(), guardado.getPatente()));
        return guardado;
    }

    public Vehiculo modificar(Long id, Vehiculo datos) {
        Vehiculo existente = consultar(id);
        datos.setPatente(existente.getPatente());
        normalizarCampos(datos);
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

    /**
     * Lista vehículos aplicando los filtros opcionales del contrato.
     * activo y tipo en null = sin filtro; ids vacío = sin filtro por ids.
     */
    public List<Vehiculo> listar(Boolean activo, TipoVehiculo tipo, List<Long> ids) {
        List<Vehiculo> base = activo == null ? repository.findAll() : repository.findByActivo(activo);
        return base.stream()
                .filter(v -> tipo == null || v.getTipoVehiculo() == tipo)
                .filter(v -> ids == null || ids.isEmpty() || ids.contains(v.getId()))
                .toList();
    }

    public void actualizarEstado(Long id, EstadoVehiculo nuevoEstado) {
        Vehiculo v = consultar(id);
        if (v.getVerificacion() == VerificacionVehiculo.ROBADO) {
            throw new VehiculoInvalidoException("El vehículo está denunciado como robado");
        }
        v.setEstado(nuevoEstado);
        repository.save(v);
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
        if (v.getTipoVehiculo() == null) {
            throw new VehiculoInvalidoException("El tipo de vehículo es obligatorio");
        }
        if (v.getPrecioDiario() == null || v.getPrecioDiario() <= 0) {
            throw new VehiculoInvalidoException("El precio diario debe ser mayor a 0");
        }
        validarAnio(v.getAnio());
    }

    private void validarAnio(String anioStr) {
        if (anioStr == null || anioStr.isBlank()) {
            throw new VehiculoInvalidoException("El año es obligatorio");
        }
        int anio;
        try {
            anio = Integer.parseInt(anioStr.trim());
        } catch (NumberFormatException e) {
            throw new VehiculoInvalidoException("El año debe ser numérico");
        }
        int anioActual = java.time.Year.now().getValue();
        if (anio < 1900 || anio > anioActual + 1) {
            throw new VehiculoInvalidoException("El año debe estar entre 1900 y " + (anioActual + 1));
        }
    }

    private void normalizarCampos(Vehiculo v) {
        if (v.getPatente() != null) {
            v.setPatente(v.getPatente().trim().toUpperCase().replace(" ", ""));
        }
        if (v.getMarca() != null) v.setMarca(v.getMarca().trim());
        if (v.getModelo() != null) v.setModelo(v.getModelo().trim());
        if (v.getColor() != null) v.setColor(v.getColor().trim());
        if (v.getAnio() != null) v.setAnio(v.getAnio().trim());
    }

    public Vehiculo reactivar(Long id) {
        Vehiculo v = consultar(id);
        if (Boolean.TRUE.equals(v.getActivo())) {
            throw new VehiculoYaActivoException(id);
        }
        if (v.getVerificacion() == VerificacionVehiculo.ROBADO) {
            throw new VehiculoInvalidoException("El vehículo está denunciado como robado y no puede reactivarse");
        }
        v.setActivo(true);
        return repository.save(v);
    }

    public List<Vehiculo> consultarDisponibilidadSimple(
            String tipoVehiculo, String marca, String modelo,
            Double precioMin, Double precioMax, List<Long> idsOcupados) {

        if (precioMin != null && precioMax != null && precioMin > precioMax) {
            throw new VehiculoInvalidoException("precioMin no puede ser mayor a precioMax");
        }

        return repository.findByActivo(true).stream().filter(v -> v.getVerificacion() != VerificacionVehiculo.ROBADO)
                .filter(v -> tipoVehiculo == null || v.getTipoVehiculo().name().equals(tipoVehiculo))
                .filter(v -> marca == null || v.getMarca().toLowerCase().contains(marca.toLowerCase()))
                .filter(v -> modelo == null || v.getModelo().toLowerCase().contains(modelo.toLowerCase()))
                .filter(v -> precioMin == null || v.getPrecioDiario() >= precioMin)
                .filter(v -> precioMax == null || v.getPrecioDiario() <= precioMax)
                .filter(v -> idsOcupados == null || !idsOcupados.contains(v.getId()))
                .toList();
    }

    @Transactional
    public void aplicarVerificacion(Long id, VerificacionVehiculo resultado) {
        Vehiculo v = consultar(id);
        if (v.getVerificacion() == resultado || v.getVerificacion() == VerificacionVehiculo.ROBADO) {
            return; // idempotente, y ROBADO es definitivo
        }
        v.setVerificacion(resultado);
        if (resultado == VerificacionVehiculo.ROBADO) {
            v.setActivo(false); // inhabilitado para nuevas operaciones
        }
        repository.save(v);
    }


}