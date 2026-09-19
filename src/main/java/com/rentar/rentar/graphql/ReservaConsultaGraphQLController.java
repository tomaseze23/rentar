package com.rentar.rentar.graphql;

import com.rentar.rentar.dtos.HistorialAlquilerResponse;
import com.rentar.rentar.dtos.ReservaConsultaResponse;
import com.rentar.rentar.dtos.ReservaFiltro;
import com.rentar.rentar.services.ReservaConsultaService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ReservaConsultaGraphQLController {
    private final ReservaConsultaService servicio;

    public ReservaConsultaGraphQLController(ReservaConsultaService servicio) {
        this.servicio = servicio;
    }

    @QueryMapping
    public List<ReservaConsultaResponse> reservas(@Argument ReservaFiltro filtro) {
        return servicio.consultar(filtro, SecurityContextHolder.getContext().getAuthentication());
    }

    @QueryMapping
    public List<HistorialAlquilerResponse> historialAlquileres() {
        return servicio.historial(SecurityContextHolder.getContext().getAuthentication());
    }
}
