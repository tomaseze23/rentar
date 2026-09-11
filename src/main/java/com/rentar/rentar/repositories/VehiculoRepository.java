package com.rentar.rentar.repositories;

import com.rentar.rentar.entities.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo,Long> {
    Optional<Vehiculo>findByPatente(String patente);

}
