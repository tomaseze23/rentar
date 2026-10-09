package com.rentar.rentar.services.implementation;

import com.rentar.rentar.dtos.VehiculoRequest;
import com.rentar.rentar.dtos.VehiculoResponse;
import com.rentar.rentar.dtos.VehiculoUpdateRequest;
import com.rentar.rentar.entities.EstadoVehiculo;
import com.rentar.rentar.entities.Vehiculo;
import com.rentar.rentar.exceptions.DuplicateResourceException;
import com.rentar.rentar.exceptions.ResourceNotFoundException;
import com.rentar.rentar.repositories.VehiculoRepository;
import com.rentar.rentar.services.VehiculoService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.List;

@Service
public class VehiculoServiceImpl implements VehiculoService {

    private static final int ANIO_MINIMO = 1900;

    private final VehiculoRepository repo;

    public VehiculoServiceImpl(VehiculoRepository repo){
        this.repo = repo;
    }

    @Override
    @Transactional 
    public VehiculoResponse crear(VehiculoRequest request) {
        String patente = normalizarPatente(request.getPatente());
        if(repo.existsByPatente(patente)){
            throw new DuplicateResourceException("Ya existe un vehículo con la patente: " + patente);
        } 
        validarAnio(request.getAnio());

        Vehiculo v = new Vehiculo();
        v.setPatente(patente);
        v.setMarca(request.getMarca().trim());
        v.setModelo(request.getModelo().trim());
        v.setAnio(request.getAnio().trim());
        v.setColor(textoOpcional(request.getColor()));
        v.setTipoVehiculo(request.getTipoVehiculo());
        v.setPrecioDiario(request.getPrecioDiario());        
        v.setEstado(EstadoVehiculo.DISPONIBLE);
        v.setActivo(true);
        return VehiculoResponse.fromEntity(repo.save(v));
    }

    @Override
    @Transactional 
    public VehiculoResponse modificar(Long id, VehiculoUpdateRequest request){
        Vehiculo v = obtenerOrThrow(id);
        if (request.getPatente() != null && 
                !request.getPatente().isBlank()  && 
                !normalizarPatente(request.getPatente()).equals(v.getPatente())) {
            throw new IllegalArgumentException("La patente no puede modificarse una vez registrado el vehículo");
        }
        validarAnio(request.getAnio());
        
        v.setMarca(request.getMarca().trim());
        v.setModelo(request.getModelo().trim());
        v.setAnio(request.getAnio().trim());
        v.setColor(textoOpcional(request.getColor()));
        v.setTipoVehiculo(request.getTipoVehiculo());
        v.setPrecioDiario(request.getPrecioDiario());    
        return VehiculoResponse.fromEntity(repo.save(v));
    }

    @Override
    @Transactional 
    public void bajaLogica(Long id) {
        Vehiculo v = obtenerOrThrow(id);
        v.setActivo(false);
        repo.save(v);
    }

    @Override 
    @Transactional 
    public VehiculoResponse reactivar (Long id) {
        Vehiculo v = obtenerOrThrow(id);
        if(Boolean.TRUE.equals(v.getActivo())){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El vehículo ya se encuentra activo.");
        }
        v.setActivo(true);
        return VehiculoResponse.fromEntity(repo.save(v));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar(Boolean activo) {
        Sort orden = Sort.by("marca", "modelo", "patente");
        List<Vehiculo> vehiculos = activo == null ? repo.findAll(orden) : repo.findByActivo(activo, orden);
        return vehiculos.stream().map(VehiculoResponse::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehiculoResponse consultar(Long id) {
        return VehiculoResponse.fromEntity(obtenerOrThrow(id));
    }

    private Vehiculo obtenerOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con id: " + id));
    }

    private String normalizarPatente(String patente) {
        return patente.replace(" ", "").toUpperCase();
    }

    private void validarAnio(String anio) {
        int valor = Integer.parseInt(anio.trim());
        int maximo = Year.now().getValue() + 1;
        if (valor < ANIO_MINIMO || valor > maximo) {
            throw new IllegalArgumentException("El año debe estar entre " + ANIO_MINIMO + " y " + maximo);
        }
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }   
}
