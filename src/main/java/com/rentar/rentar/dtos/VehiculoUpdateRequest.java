package com.rentar.rentar.dtos;

import com.rentar.rentar.entities.TipoVehiculo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para modificar un vehículo.
 * La patente no puede modificarse: si se envía, tiene que coincidir con la registrada.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos modificables de un vehículo")
public class VehiculoUpdateRequest {

    @Schema(description = "Opcional. Si se envía debe coincidir con la patente registrada (no se puede modificar)",
            example = "AB123CD")
    private String patente;

    @NotBlank(message = "La marca es obligatoria")
    @Schema(description = "Marca", example = "Toyota")
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Schema(description = "Modelo", example = "Corolla")
    private String modelo;

    @NotBlank(message = "El año es obligatorio")
    @Pattern(regexp = "^\\d{4}$", message = "El año debe tener 4 dígitos")
    @Schema(description = "Año de fabricación", example = "2022")
    private String anio;

    @Schema(description = "Color (opcional)", example = "Gris")
    private String color;

    @NotNull(message = "El tipo de vehículo es obligatorio")
    @Schema(description = "Tipo de vehículo")
    private TipoVehiculo tipoVehiculo;

    @NotNull(message = "El precio diario es obligatorio")
    @Positive(message = "El precio diario debe ser mayor a 0")
    @Schema(description = "Precio por día", example = "45000")
    private Double precioDiario;
}