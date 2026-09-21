package com.rentar.rentar.repositories;

import com.rentar.rentar.entities.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo,Long>, JpaSpecificationExecutor<Vehiculo> {
    Optional<Vehiculo>findByPatente(String patente);

}
