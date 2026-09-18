package com.rentar.rentar.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentar.rentar.dtos.ReservaRequest;
import com.rentar.rentar.dtos.ReservaResponse;
import com.rentar.rentar.services.ReservaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservaController.class)
@AutoConfigureMockMvc(addFilters = false) // Deshabilita los filtros de Spring Security y CSRF en el test
class ReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @MockitoBean
    private ReservaService reservaService;

    @Test
    @DisplayName("Debe retornar 201 Created cuando la reserva es exitosa")
    void crearReserva_Exitoso() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusDays(2);
        LocalDateTime fin = LocalDateTime.now().plusDays(5);

        ReservaRequest request = new ReservaRequest(1L, 2L, inicio, fin);
        ReservaResponse response = new ReservaResponse(
                100L, 1L, 2L, inicio, fin,
                new BigDecimal("25000.00"),
                new BigDecimal("75000.00"),
                "CONFIRMADA"
        );

        Mockito.when(reservaService.crearReserva(any(ReservaRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.importeTotal").value(75000.00));
    }

    @Test
    @DisplayName("Debe retornar 400 Bad Request si faltan datos requeridos (Bean Validation)")
    void crearReserva_CamposNulos() throws Exception {
        ReservaRequest requestInvalido = new ReservaRequest(null, null, null, null);

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Debe retornar 409 Conflict si el vehículo ya está reservado")
    void crearReserva_VehiculoOcupado() throws Exception {
        LocalDateTime inicio = LocalDateTime.now().plusDays(2);
        LocalDateTime fin = LocalDateTime.now().plusDays(4);
        ReservaRequest request = new ReservaRequest(1L, 2L, inicio, fin);

        Mockito.when(reservaService.crearReserva(any(ReservaRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Vehículo ocupado"));

        mockMvc.perform(post("/api/v1/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}