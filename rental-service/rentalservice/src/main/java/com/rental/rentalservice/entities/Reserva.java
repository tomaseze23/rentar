package com.rental.rentalservice.entities;

// Importaciones necesarias que faltan
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "vehiculo_id", nullable = false)
    private Long vehiculoId;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false)
    private LocalDateTime fechaFin;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioDiario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importeTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado_mantenimiento", nullable = false, length = 20)
    private ResultadoMantenimiento resultadoMantenimiento = ResultadoMantenimiento.PENDIENTE;

    public Reserva(Long id, Long clienteId, Long vehiculoId, LocalDateTime fechaInicio, LocalDateTime fechaFin,
                   BigDecimal precioDiario, BigDecimal importeTotal, EstadoReserva estado) {
        this.id = id;
        this.clienteId = clienteId;
        this.vehiculoId = vehiculoId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.precioDiario = precioDiario;
        this.importeTotal = importeTotal;
        this.estado = estado;
        this.resultadoMantenimiento = ResultadoMantenimiento.PENDIENTE;
    }
}