package com.rentar.rentar.controllers;

import com.rentar.rentar.dtos.VehiculoRequest;
import com.rentar.rentar.dtos.VehiculoResponse;
import com.rentar.rentar.dtos.VehiculoUpdateRequest;
import com.rentar.rentar.entities.EstadoVehiculo;
import com.rentar.rentar.entities.TipoVehiculo;
import com.rentar.rentar.exceptions.DuplicateResourceException;
import com.rentar.rentar.exceptions.ResourceNotFoundException;
import com.rentar.rentar.security.JwtService;
import com.rentar.rentar.services.VehiculoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehiculoController.class)
@WithMockUser
class VehiculoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehiculoService service;

    @MockitoBean
    private JwtService jwtService;

    private static final String ALTA_VALIDA = """
            {"patente":"AB123CD","marca":"Toyota","modelo":"Corolla","anio":"2022",
             "color":"Gris","tipoVehiculo":"SEDAN","precioDiario":45000}""";

    private VehiculoResponse respuesta(boolean activo) {
        return new VehiculoResponse(1L, "AB123CD", "Toyota", "Corolla", "2022", "Gris",
                TipoVehiculo.SEDAN, 45000.0, EstadoVehiculo.DISPONIBLE, activo);
    }

    @Test
    void crear_valido_devuelve201() throws Exception {
        when(service.crear(any(VehiculoRequest.class))).thenReturn(respuesta(true));

        mockMvc.perform(post("/api/vehiculos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patente").value("AB123CD"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"));
    }

    @Test
    void crear_sinCamposObligatorios_devuelve400ConElDetalle() throws Exception {
        mockMvc.perform(post("/api/vehiculos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"color\":\"Rojo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.patente").exists())
                .andExpect(jsonPath("$.errores.marca").exists())
                .andExpect(jsonPath("$.errores.modelo").exists())
                .andExpect(jsonPath("$.errores.anio").exists())
                .andExpect(jsonPath("$.errores.tipoVehiculo").exists())
                .andExpect(jsonPath("$.errores.precioDiario").exists());
        verify(service, never()).crear(any());
    }

    @Test
    void crear_conPrecioNegativoOAnioInvalido_devuelve400() throws Exception {
        String body = ALTA_VALIDA.replace("45000", "-10").replace("\"2022\"", "\"22\"");

        mockMvc.perform(post("/api/vehiculos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precioDiario").value("El precio diario debe ser mayor a 0"))
                .andExpect(jsonPath("$.errores.anio").value("El año debe tener 4 dígitos"));
    }

    @Test
    void crear_conTipoInexistente_devuelve400() throws Exception {
        mockMvc.perform(post("/api/vehiculos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA.replace("SEDAN", "TANQUE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void crear_conPatenteDuplicada_devuelve409() throws Exception {
        when(service.crear(any(VehiculoRequest.class)))
                .thenThrow(new DuplicateResourceException("Ya existe un vehículo con la patente: AB123CD"));

        mockMvc.perform(post("/api/vehiculos").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un vehículo con la patente: AB123CD"));
    }

    @Test
    void modificar_intentandoCambiarLaPatente_devuelve400() throws Exception {
        when(service.modificar(eq(1L), any(VehiculoUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("La patente no puede modificarse una vez registrado el vehículo"));

        mockMvc.perform(put("/api/vehiculos/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA.replace("AB123CD", "ZZ999ZZ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La patente no puede modificarse una vez registrado el vehículo"));
    }

    @Test
    void consultar_inexistente_devuelve404() throws Exception {
        when(service.consultar(99L)).thenThrow(new ResourceNotFoundException("Vehículo no encontrado con id: 99"));

        mockMvc.perform(get("/api/vehiculos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reactivar_devuelve200ConElVehiculoActivo() throws Exception {
        when(service.reactivar(1L)).thenReturn(respuesta(true));

        mockMvc.perform(patch("/api/vehiculos/1/reactivar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }
}