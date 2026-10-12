package com.rental.rentalservice.repositories;

import com.rental.rentalservice.entities.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long>, JpaSpecificationExecutor<Reserva> {
    List<Reserva> findByClienteId(Long clienteId);

    @Query("""
            SELECT COUNT(r) > 0 FROM Reserva r
            WHERE r.vehiculoId = :vehiculoId
              AND r.estado = com.rental.rentalservice.entities.EstadoReserva.CONFIRMADA
              AND r.fechaInicio < :fechaFin
              AND r.fechaFin > :fechaInicio
            """)
    boolean existsConfirmedOverlap(
            @Param("vehiculoId") Long vehiculoId,
            @Param("fechaInicio") java.time.LocalDateTime fechaInicio,
            @Param("fechaFin") java.time.LocalDateTime fechaFin);
}