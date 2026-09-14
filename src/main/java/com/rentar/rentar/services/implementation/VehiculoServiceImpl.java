package com.rentar.rentar.services.implementation;

import com.rentar.rentar.entities.EstadoVehiculo;
import com.rentar.rentar.entities.Vehiculo;
import com.rentar.rentar.repositories.VehiculoRepository;
import com.rentar.rentar.services.VehiculoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehiculoServiceImpl implements VehiculoService {

    @Autowired
    private VehiculoRepository repo;

    @Override
    public Vehiculo crear(Vehiculo v) {
        v.setId(null);
        if(repo.findByPatente(v.getPatente()).isPresent()){
            throw new IllegalArgumentException("la patente ya existe");
        }
        v.setEstado(EstadoVehiculo.DISPONIBLE);
        v.setActivo(true);
        return repo.save(v);
    }

    @Override
    public Vehiculo modificar(Long id, Vehiculo datos) {
        Vehiculo v = repo.findById(id).orElseThrow(()->new EntityNotFoundException("vehiculo no encontrado"));
        v.setMarca(datos.getMarca());
        v.setModelo(datos.getModelo());
        v.setAnio(datos.getAnio());
        v.setColor(datos.getColor());
        v.setTipoVehiculo(datos.getTipoVehiculo());
        v.setPrecioDiario(datos.getPrecioDiario());
        return repo.save(v);
    }

    @Override
    public void bajaLogica(Long id) {
        Vehiculo v= repo.findById(id).orElseThrow(()->new EntityNotFoundException("vehiculo no encontrado"));
        v.setActivo(false);
        repo.save(v);
    }

    @Override
    public List<Vehiculo> listar(){
        return repo.findAll();
    }

    @Override
    public Vehiculo consultar(Long id) {
        return repo.findById(id).orElseThrow(()->new EntityNotFoundException("vehiculo no encontrado"));
    }
}
