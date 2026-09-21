package com.rentar.rentar.graphql;

import com.rentar.rentar.dtos.ClienteRequest;
import com.rentar.rentar.dtos.ClienteResponse;
import com.rentar.rentar.dtos.ClienteUpdateRequest;
import com.rentar.rentar.services.ClienteService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Controller
@Validated
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ClienteGraphQLController {

    private final ClienteService clienteService;

    public ClienteGraphQLController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @QueryMapping
    public List<ClienteResponse> clientes(@Argument Boolean incluirInactivos) {
        boolean incluir = incluirInactivos != null && incluirInactivos;
        return incluir ? clienteService.listarTodos() : clienteService.listarActivos();
    }

    @QueryMapping
    public ClienteResponse cliente(@Argument Long id) {
        return clienteService.buscarPorId(id);
    }

    @MutationMapping
    public ClienteResponse crearCliente(@Argument @Valid ClienteRequest input) {
        return clienteService.crear(input);
    }

    @MutationMapping
    public ClienteResponse actualizarCliente(@Argument Long id, @Argument @Valid ClienteUpdateRequest input) {
        return clienteService.actualizar(id, input);
    }

    @MutationMapping
    public boolean eliminarCliente(@Argument Long id) {
        clienteService.eliminar(id);
        return true;
    }
}