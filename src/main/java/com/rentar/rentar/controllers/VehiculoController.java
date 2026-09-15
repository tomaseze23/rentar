package com.rentar.rentar.controllers;


import com.rentar.rentar.entities.Vehiculo;
import com.rentar.rentar.services.VehiculoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    @Autowired
    private VehiculoService service;

    @PostMapping
    public ResponseEntity<Vehiculo>crear(@RequestBody Vehiculo v){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(v));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> modificar(@PathVariable Long id, @RequestBody Vehiculo v) {
        return ResponseEntity.ok(service.modificar(id, v));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> baja(@PathVariable Long id) {
        service.bajaLogica(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Vehiculo>> listar(
        @RequestParam(required = false)Boolean activo){
        return ResponseEntity.ok(service.listar(activo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehiculo> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(service.consultar(id));
    }





}
