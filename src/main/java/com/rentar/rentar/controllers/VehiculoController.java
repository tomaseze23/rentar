package com.rentar.rentar.controllers;


import com.rentar.rentar.entities.Vehiculo;
import com.rentar.rentar.services.VehiculoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
@Tag(name = "Vehiculos", description = "ABM de vehiculos")
public class VehiculoController {

    @Autowired
    private VehiculoService service;

    @Operation(summary = "Alta de un vehículo")
    @PostMapping
    public ResponseEntity<Vehiculo> crear(@RequestBody Vehiculo v) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(v));
    }

    @Operation(summary = "Actualiza los datos de un vehículo")
    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> modificar(@PathVariable Long id, @RequestBody Vehiculo v) {
        return ResponseEntity.ok(service.modificar(id, v));
    }

    @Operation(summary = "Baja lógica de un vehículo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> baja(@PathVariable Long id) {
        service.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lista vehículos. Por defecto solo activos; activo=false trae inactivos también")
    @GetMapping
    public ResponseEntity<List<Vehiculo>> listar(@RequestParam(required = false) Boolean activo) {
        return ResponseEntity.ok(service.listar(activo));
    }

    @Operation(summary = "Busca un vehículo por id")
    @GetMapping("/{id}")
    public ResponseEntity<Vehiculo> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultar(id));
    }
}






