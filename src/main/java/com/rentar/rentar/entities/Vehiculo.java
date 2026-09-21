package com.rentar.rentar.entities;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Vehículo de la flota de Rentar")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Id autogenerado", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(unique = true, nullable = false)
    @Schema(description = "Patente única del vehículo", example = "AB123CD")
    private String patente;

    @Column(nullable = false)
    @Schema(description = "Marca", example = "Toyota")
    private String marca;

    @Column(nullable = false)
    @Schema(description = "Modelo", example = "Corolla")
    private String modelo;

    @Column(nullable = false)
    @Schema(description = "Año de fabricación", example = "2022")
    private String anio;

    @Schema(description = "Color", example = "Gris")
    private String color;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Tipo de vehículo")
    private TipoVehiculo tipoVehiculo;

    @Schema(description = "Precio por día", example = "45000")
    private Double precioDiario;

    @Enumerated(EnumType.STRING)
    @Schema(description = "Estado (DISPONIBLE, RESERVADO, EN_ALQUILER)")
    private EstadoVehiculo estado;

    @Schema(description = "true = activo, false = baja lógica")
    private Boolean activo;
}











