package com.rentar.rentar.repositories;

import com.rentar.rentar.entities.EstadoReserva;
import com.rentar.rentar.entities.EstadoVehiculo;
import com.rentar.rentar.entities.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @Query("""
        SELECT COUNT(r) > 0 FROM Reserva r
        WHERE r.vehiculo.id = :vehiculoId
          AND r.estado = :estado
          AND r.fechaInicio < :fechaFin
          AND r.fechaFin > :fechaInicio
    """)
    boolean existsSolapamiento(
            @Param("vehiculoId") Long vehiculoId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("estado") EstadoReserva estado
    );
}