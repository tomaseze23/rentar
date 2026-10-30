package com.rentar.vehicleservice.repositories;

import com.rentar.vehicleservice.entities.EstadoSolicitudIncorporacion;
import com.rentar.vehicleservice.entities.SolicitudIncorporacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SolicitudIncorporacionRepository extends JpaRepository<SolicitudIncorporacion, Long> {

    boolean existsByPatenteAndEstado(String patente, EstadoSolicitudIncorporacion estado);

    List<SolicitudIncorporacion> findAllByOrderByIdDesc();

    List<SolicitudIncorporacion> findByClienteIdOrderByIdDesc(Long clienteId);

    /**
     * Lee la solicitud bloqueando la fila hasta el fin de la transacción. Las validaciones de patente y titularidad
     * llegan por colas distintas (en hilos distintos): sin el bloqueo, una podría pisar el resultado de la otra.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudIncorporacion s where s.id = :id")
    Optional<SolicitudIncorporacion> buscarParaActualizar(@Param("id") Long id);
}
