package com.rental.rentalservice.repositories;

import com.rental.rentalservice.entities.SolicitudMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SolicitudMantenimientoRepository extends JpaRepository<SolicitudMantenimiento, Long> {
    Optional<SolicitudMantenimiento> findByReservaId(Long reservaId);
}
