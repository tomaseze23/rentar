package com.rentar.rentar.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rentar.rentar.dtos.ClienteRequest;
import com.rentar.rentar.dtos.ClienteResponse;
import com.rentar.rentar.dtos.ClienteUpdateRequest;
import com.rentar.rentar.exceptions.DuplicateResourceException;
import com.rentar.rentar.exceptions.ResourceNotFoundException;
import com.rentar.rentar.services.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
@WithMockUser
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private ClienteService clienteService;

    @Test
    void crear_deberiaDevolver201ConClienteCreado() throws Exception {
        ClienteRequest request = new ClienteRequest();
        request.setEmail("juan@mail.com");
        request.setPassword("1234");
        request.setDocumento("30111222");
        request.setNombre("Juan");
        request.setApellido("Pérez");
        request.setFechaNacimiento(LocalDate.of(1990, 1, 1));

        when(clienteService.crear(any(ClienteRequest.class)))
                .thenAnswer(inv -> {
                    ClienteRequest r = inv.getArgument(0);
                    return ClienteResponse.fromEntity(buildClienteEntity(1L, 1L, r.getEmail(), r.getDocumento(), r.getNombre(), r.getApellido()));
                });

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("juan@mail.com"))
                .andExpect(jsonPath("$.documento").value("30111222"));
    }

    @Test
    void crear_deberiaDevolver400SiFaltaEmail() throws Exception {
        ClienteRequest request = new ClienteRequest();
        request.setPassword("1234");
        request.setDocumento("30111222");
        request.setNombre("Juan");
        request.setApellido("Pérez");

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_deberiaDevolver409SiEmailDuplicado() throws Exception {
        ClienteRequest request = new ClienteRequest();
        request.setEmail("dup@mail.com");
        request.setPassword("1234");
        request.setDocumento("30111222");
        request.setNombre("Juan");
        request.setApellido("Pérez");

        when(clienteService.crear(any(ClienteRequest.class)))
                .thenThrow(new DuplicateResourceException("Ya existe un usuario con el email: dup@mail.com"));

        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void listar_deberiaDevolverListaDeActivosPorDefecto() throws Exception {
        when(clienteService.listarActivos()).thenReturn(List.of(
                ClienteResponse.fromEntity(buildClienteEntity(1L, 1L, "a@mail.com", "111", "Ana", "Gómez"))
        ));

        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ana"));
    }

    @Test
    void buscarPorId_deberiaDevolver404SiNoExiste() throws Exception {
        when(clienteService.buscarPorId(eq(999L)))
                .thenThrow(new ResourceNotFoundException("Cliente no encontrado con id: 999"));

        mockMvc.perform(get("/api/clientes/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizar_deberiaDevolver200ConDatosActualizados() throws Exception {
        ClienteUpdateRequest update = new ClienteUpdateRequest();
        update.setNombre("Nuevo");
        update.setApellido("Apellido");

        when(clienteService.actualizar(eq(1L), any(ClienteUpdateRequest.class)))
                .thenReturn(ClienteResponse.fromEntity(buildClienteEntity(1L, 1L, "a@mail.com", "111", "Nuevo", "Apellido")));

        mockMvc.perform(put("/api/clientes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo"));
    }

    @Test
    void eliminar_deberiaDevolver204() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    private com.rentar.rentar.entities.Cliente buildClienteEntity(
            Long clienteId, Long usuarioId, String email, String documento, String nombre, String apellido) {
        com.rentar.rentar.entities.Usuario usuario =
                new com.rentar.rentar.entities.Usuario(email, "hash", "CLIENTE");
        usuario.setId(usuarioId);

        com.rentar.rentar.entities.Cliente cliente =
                new com.rentar.rentar.entities.Cliente(usuario, documento, nombre, apellido, null, null);
        cliente.setId(clienteId);
        return cliente;
    }
}