package com.rentar.vehicleservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Solicitud de un cliente para incorporar un vehículo propio. Guarda los datos del vehículo porque el evento
 * vehiculos.externos.registrados solo lleva vehiculoExternoId (= id), clienteId y patente.
 */
@Entity
@Table(name = "solicitud_incorporacion")
@NoArgsConstructor
@Getter
@Setter
public class SolicitudIncorporacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private String patente;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private String anio;

    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVehiculo tipoVehiculo;

    @Column(nullable = false, columnDefinition = "NUMERIC(12,2)")
    private Double precioDiario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitudIncorporacion estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoValidacion validacionPatente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoValidacion validacionTitularidad;

    // Evita publicar vehiculos.externos.validacion.completada más de una vez si llega un resultado duplicado
    @Column(nullable = false)
    private Boolean completadaPublicada;

    // Vehículo creado, solo cuando la solicitud quedó INCORPORADA
    private Long vehiculoId;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    /** Vehículo con los datos propuestos (sin id ni estado), para validarlo o crearlo. */
    public Vehiculo comoVehiculo() {
        Vehiculo v = new Vehiculo();
        v.setPatente(patente);
        v.setMarca(marca);
        v.setModelo(modelo);
        v.setAnio(anio);
        v.setColor(color);
        v.setTipoVehiculo(tipoVehiculo);
        v.setPrecioDiario(precioDiario);
        return v;
    }

    /** Copia los datos del vehículo ya normalizados (patente en mayúsculas, sin espacios, etc.). */
    public void tomarDatosDe(Vehiculo v) {
        patente = v.getPatente();
        marca = v.getMarca();
        modelo = v.getModelo();
        anio = v.getAnio();
        color = v.getColor();
        tipoVehiculo = v.getTipoVehiculo();
        precioDiario = v.getPrecioDiario();
    }
}
