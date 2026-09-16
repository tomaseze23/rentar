package com.rentar.rentar.controllers;

import com.rentar.rentar.dtos.ReservaRequest;
import com.rentar.rentar.dtos.ReservaResponse;
import com.rentar.rentar.services.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Endpoints de gestión de reservas de vehículos")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @Operation(summary = "Alta de reserva", description = "Permite a un cliente reservar un vehículo para un período determinado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reserva creada exitosamente en estado CONFIRMADA"),
            @ApiResponse(responseCode = "400", description = "Fechas inválidas o datos de entrada incorrectos"),
            @ApiResponse(responseCode = "404", description = "Cliente o Vehículo no encontrado"),
            @ApiResponse(responseCode = "409", description = "El vehículo ya se encuentra reservado en el rango solicitado"),
            @ApiResponse(responseCode = "422", description = "Cliente o vehículo inactivo")
    })
    public ResponseEntity<ReservaResponse> crearReserva(@Valid @RequestBody ReservaRequest request) {
        ReservaResponse nuevaReserva = reservaService.crearReserva(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaReserva);
    }
}