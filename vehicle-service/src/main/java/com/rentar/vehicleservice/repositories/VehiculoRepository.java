package com.rentar.vehicleservice.repositories;

import com.rentar.vehicleservice.entities.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    boolean existsByPatente(String patente);
    List<Vehiculo> findByActivo(boolean activo);
}