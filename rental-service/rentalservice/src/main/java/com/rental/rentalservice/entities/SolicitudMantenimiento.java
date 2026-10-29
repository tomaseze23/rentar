package com.rental.rentalservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "solicitudes_mantenimiento", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mantenimiento_reserva", columnNames = "reserva_id")
})
public class SolicitudMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "vehiculo_id", nullable = false)
    private Long vehiculoId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResultadoMantenimiento resultado;

    public SolicitudMantenimiento(Long reservaId, Long vehiculoId, LocalDateTime fechaInicio,
                                  LocalDateTime fechaFin) {
        this.reservaId = reservaId;
        this.vehiculoId = vehiculoId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.resultado = ResultadoMantenimiento.PENDIENTE;
    }
}
