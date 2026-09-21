package com.rentar.rentar.dtos;

import com.rentar.rentar.entities.TipoVehiculo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DisponibilidadFiltro {
    private String fechaInicio;
    private String fechaFin;
    private TipoVehiculo tipoVehiculo;
    private String marca;
    private String modelo;
    private Double precioMin;
    private Double precioMax;
}
