package com.rentar.rentar.entities;

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
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String patente;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private String anio;

    private String color;

    @Enumerated(EnumType.STRING)
    private TipoVehiculo tipoVehiculo;

    private Double precioDiario;

    @Enumerated(EnumType.STRING)
    private EstadoVehiculo estado;

    private Boolean activo;












}
