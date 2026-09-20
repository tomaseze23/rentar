package com.rentar.rentar.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HistorialAlquilerResponse {
    private Long id;
    private String vehiculo;
    private String patente;
    private String fechaInicio;
    private String fechaFin;
    private Long cantidadDias;
    private String importeTotal;
    private String estado;
}
