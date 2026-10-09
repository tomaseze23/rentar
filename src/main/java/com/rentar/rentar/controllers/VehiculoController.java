package com.rentar.rentar.controllers;


import com.rentar.rentar.dtos.VehiculoRequest;
import com.rentar.rentar.dtos.VehiculoResponse;
import com.rentar.rentar.dtos.VehiculoUpdateRequest;
import com.rentar.rentar.entities.Vehiculo;
import com.rentar.rentar.services.VehiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
@Tag(name = "Vehiculos", description = "ABM de vehiculos")
public class VehiculoController {

    private final VehiculoService service;

    public VehiculoController(VehiculoService service){
        this.service = service;
    }

    @Operation(summary = "Alta de un vehículo", description = "El vehículo queda activo y en estado DISPONIBLE.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vehículo creado"),
            @ApiResponse(responseCode = "400", description = "Datos obligatorios faltantes o inválidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe un vehículo con esa patente")
    })    
    @PostMapping
    public ResponseEntity<VehiculoResponse> crear(@Valid @RequestBody VehiculoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @Operation(summary = "Actualiza los datos de un vehículo", description = "La patente no puede modificarse.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehículo actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o intento de modificar la patente"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<VehiculoResponse> modificar(@PathVariable Long id, @Valid @RequestBody VehiculoUpdateRequest request) {
        return ResponseEntity.ok(service.modificar(id, request));
    }

    @Operation(summary = "Baja lógica de un vehículo", description= "Lo marca inactivo. No admite nuevas reservas. Las reservas existentes se mantienen.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vehículo dado de baja"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> baja(@PathVariable Long id) {
        service.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }


    @Operation(summary = "Reactiva un vehículo dado de baja", description = "Vuelve a quedar disponible para nuevas reservas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehículo reactivado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado"),
            @ApiResponse(responseCode = "409", description = "El vehículo ya estaba activo")
    })
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<VehiculoResponse> reactivar(@PathVariable Long id){
        return ResponseEntity.ok(service.reactivar(id));
    }

    @Operation(summary = "Lista vehículos.", description = "Sin parámetro devuelve todos; activo=true solo los activos y activo=false solo los dados de baja.")
    @GetMapping
    public ResponseEntity<List<VehiculoResponse>> listar(
            @Parameter(description = "Filtra por activo/inactivo. Omitir para traer todos")
            @RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(service.listar(activo));
    }

    @Operation(summary = "Busca un vehículo por id")
       @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehículo encontrado"),
            @ApiResponse(responseCode = "404", description = "Vehículo no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<VehiculoResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultar(id));
    }
}