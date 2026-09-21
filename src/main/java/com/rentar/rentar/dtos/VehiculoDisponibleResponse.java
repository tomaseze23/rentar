package com.rentar.rentar.dtos;

import com.rentar.rentar.entities.TipoVehiculo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VehiculoDisponibleResponse {
    private Long id;
    private String patente;
    private String marca;
    private String modelo;
    private String anio;
    private String color;
    private TipoVehiculo tipoVehiculo;
    private Double precioDiario;
}
