package com.rentar.rentar.controllers;

import com.rentar.rentar.dtos.ClienteRequest;
import com.rentar.rentar.dtos.ClienteResponse;
import com.rentar.rentar.dtos.ClienteUpdateRequest;
import com.rentar.rentar.services.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "ABM de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @Operation(summary = "Alta de un cliente (crea también su usuario asociado)")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse creado = clienteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    @Operation(summary = "Lista clientes. Por defecto solo activos; incluirInactivos=true trae todos")
    public ResponseEntity<List<ClienteResponse>> listar(
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        List<ClienteResponse> clientes = incluirInactivos
                ? clienteService.listarTodos()
                : clienteService.listarActivos();
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca un cliente por id")
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza los datos de un cliente")
    public ResponseEntity<ClienteResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody ClienteUpdateRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Baja lógica de un cliente (y su usuario asociado)")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
