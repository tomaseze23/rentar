package com.rentar.rentar.graphql;

import com.rentar.rentar.dtos.DisponibilidadFiltro;
import com.rentar.rentar.dtos.VehiculoDisponibleResponse;
import com.rentar.rentar.services.DisponibilidadService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class DisponibilidadGraphQLController {

    private final DisponibilidadService servicio;

    public DisponibilidadGraphQLController(DisponibilidadService servicio) {
        this.servicio = servicio;
    }

    @QueryMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public List<VehiculoDisponibleResponse> vehiculosDisponibles(@Argument DisponibilidadFiltro filtro) {
        return servicio.buscar(filtro);
    }
}
