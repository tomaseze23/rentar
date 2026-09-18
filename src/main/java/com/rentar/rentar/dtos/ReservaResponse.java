package com.rentar.rentar.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResponse {

    private Long id;
    private Long clienteId;
    private Long vehiculoId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private BigDecimal precioDiario;
    private BigDecimal importeTotal;
    private String estado;
}