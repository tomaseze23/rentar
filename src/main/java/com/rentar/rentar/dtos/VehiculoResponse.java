package com.rentar.rentar.dtos;

import com.rentar.rentar.entities.EstadoVehiculo;
import com.rentar.rentar.entities.TipoVehiculo;
import com.rentar.rentar.entities.Vehiculo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO de salida de un vehículo de la flota.
 */
@Getter
@AllArgsConstructor
@Schema(description = "Vehículo de la flota de Rentar")
public class VehiculoResponse {

    @Schema(description = "Id autogenerado")
    private Long id;

    @Schema(description = "Patente única del vehículo", example = "AB123CD")
    private String patente;

    @Schema(description = "Marca", example = "Toyota")
    private String marca;

    @Schema(description = "Modelo", example = "Corolla")
    private String modelo;

    @Schema(description = "Año de fabricación", example = "2022")
    private String anio;

    @Schema(description = "Color", example = "Gris")
    private String color;

    @Schema(description = "Tipo de vehículo")
    private TipoVehiculo tipoVehiculo;

    @Schema(description = "Precio por día", example = "45000")
    private Double precioDiario;

    @Schema(description = "Estado (DISPONIBLE, RESERVADO, EN_ALQUILER)")
    private EstadoVehiculo estado;

    @Schema(description = "true = activo, false = dado de baja (baja lógica)")
    private Boolean activo;

    public static VehiculoResponse fromEntity(Vehiculo v) {
        return new VehiculoResponse(v.getId(), v.getPatente(), v.getMarca(), v.getModelo(), v.getAnio(),
                v.getColor(), v.getTipoVehiculo(), v.getPrecioDiario(), v.getEstado(), v.getActivo());
    }
}