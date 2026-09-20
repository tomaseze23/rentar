package com.rentar.rentar.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReservaConsultaResponse {
    private Long id;
    private Long clienteId;
    private String cliente;
    private Long vehiculoId;
    private String vehiculo;
    private String patente;
    private String fechaInicio;
    private String fechaFin;
    private String precioDiario;
    private String importeTotal;
    private String estado;
}
