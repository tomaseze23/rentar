package com.rentar.vehicleservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
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

    // El script database/02_vehiculos.sql la crea como NUMERIC(12,2); sin esto ddl-auto=validate falla.
    @Column(columnDefinition = "NUMERIC(12,2)")
    private Double precioDiario;

    @Enumerated(EnumType.STRING)
    private EstadoVehiculo estado;

    private Boolean activo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificacionVehiculo verificacion = VerificacionVehiculo.VALIDADO;

    // Cliente dueño si el vehículo entró por incorporación externa; null para la flota de Rentar
    private Long propietarioClienteId;

}