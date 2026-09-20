package com.rentar.rentar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentar.rentar.entities.Cliente;
import com.rentar.rentar.entities.EstadoReserva;
import com.rentar.rentar.entities.Reserva;
import com.rentar.rentar.entities.TipoVehiculo;
import com.rentar.rentar.entities.Usuario;
import com.rentar.rentar.entities.Vehiculo;
import com.rentar.rentar.repositories.ClienteRepository;
import com.rentar.rentar.repositories.ReservaRepository;
import com.rentar.rentar.repositories.UsuarioRepository;
import com.rentar.rentar.repositories.VehiculoRepository;
import com.rentar.rentar.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de punta a punta (filtros de seguridad + JWT reales, base H2) de las reglas de acceso:
 * quién puede usar GraphQL de clientes y disponibilidad, y que las reservas se hagan siempre
 * a nombre del usuario autenticado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seguridad-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-secret-not-for-production-1234"
})
@DisplayName("Seguridad: roles y propiedad de las reservas")
class SeguridadIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ClienteRepository clientes;
    @Autowired private VehiculoRepository vehiculos;
    @Autowired private ReservaRepository reservas;

    private final ObjectMapper json = new ObjectMapper();

    private Cliente ana;
    private Cliente beto;
    private Vehiculo vehiculo;
    private Reserva reservaDeAna;

    private String tokenAdmin;
    private String tokenAna;
    private String tokenBeto;

    @BeforeEach
    void datos() {
        reservas.deleteAll();
        clientes.deleteAll();
        vehiculos.deleteAll();
        usuarios.deleteAll();

        usuarios.save(new Usuario("admin@test.com", "x", "ADMINISTRADOR"));
        ana = cliente("ana@test.com", "30111222", "Ana");
        beto = cliente("beto@test.com", "30333444", "Beto");

        Vehiculo v = new Vehiculo();
        v.setPatente("AAA111");
        v.setMarca("Toyota");
        v.setModelo("Corolla");
        v.setAnio("2022");
        v.setTipoVehiculo(TipoVehiculo.SEDAN);
        v.setPrecioDiario(15000.0);
        v.setActivo(true);
        vehiculo = vehiculos.save(v);

        Reserva r = new Reserva();
        r.setCliente(ana);
        r.setVehiculo(vehiculo);
        r.setFechaInicio(LocalDateTime.now().plusDays(60).withNano(0));
        r.setFechaFin(LocalDateTime.now().plusDays(62).withNano(0));
        r.setPrecioDiario(BigDecimal.valueOf(15000));
        r.setImporteTotal(BigDecimal.valueOf(30000));
        r.setEstado(EstadoReserva.CONFIRMADA);
        reservaDeAna = reservas.save(r);

        tokenAdmin = jwtService.generarToken("admin@test.com", "ADMINISTRADOR");
        tokenAna = jwtService.generarToken("ana@test.com", "CLIENTE");
        tokenBeto = jwtService.generarToken("beto@test.com", "CLIENTE");
    }

    // --- GraphQL de clientes: solo ADMINISTRADOR ---

    @Test
    @DisplayName("GraphQL clientes sin token es rechazado")
    void clientesSinToken() throws Exception {
        graphql(null, "{ clientes { id email } }", Map.of())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("GraphQL clientes con rol CLIENTE es rechazado")
    void clientesConRolCliente() throws Exception {
        graphql(tokenAna, "{ clientes { id email } }", Map.of())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GraphQL crearCliente y eliminarCliente sin token son rechazados")
    void mutationsDeClientesSinToken() throws Exception {
        graphql(null, "mutation { eliminarCliente(id: " + ana.getId() + ") }", Map.of())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));

        graphql(tokenBeto, "mutation { eliminarCliente(id: " + ana.getId() + ") }", Map.of())
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GraphQL clientes con rol ADMINISTRADOR devuelve los clientes")
    void clientesConAdmin() throws Exception {
        graphql(tokenAdmin, "{ clientes { email } }", Map.of())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.clientes.length()").value(2));
    }

    // --- GraphQL de disponibilidad: solo CLIENTE ---

    private static final String QUERY_DISPONIBILIDAD = """
            query($filtro: DisponibilidadFiltro!) {
              vehiculosDisponibles(filtro: $filtro) { patente marca modelo anio tipoVehiculo precioDiario }
            }""";

    @Test
    @DisplayName("Disponibilidad con rol CLIENTE devuelve vehículos libres en el período")
    void disponibilidadConCliente() throws Exception {
        Map<String, Object> filtro = Map.of(
                "fechaInicio", LocalDateTime.now().plusDays(20).withNano(0).toString(),
                "fechaFin", LocalDateTime.now().plusDays(21).withNano(0).toString());

        graphql(tokenAna, QUERY_DISPONIBILIDAD, Map.of("filtro", filtro))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.vehiculosDisponibles[0].patente").value("AAA111"));
    }

    @Test
    @DisplayName("Disponibilidad excluye el vehículo que ya tiene una reserva confirmada en el período")
    void disponibilidadExcluyeOcupados() throws Exception {
        Map<String, Object> filtro = Map.of(
                "fechaInicio", LocalDateTime.now().plusDays(60).withNano(0).toString(),
                "fechaFin", LocalDateTime.now().plusDays(61).withNano(0).toString());

        graphql(tokenAna, QUERY_DISPONIBILIDAD, Map.of("filtro", filtro))
                .andExpect(jsonPath("$.data.vehiculosDisponibles.length()").value(0));
    }

    @Test
    @DisplayName("Disponibilidad con ADMINISTRADOR o sin token es rechazada")
    void disponibilidadSoloParaClientes() throws Exception {
        Map<String, Object> filtro = Map.of(
                "fechaInicio", LocalDateTime.now().plusDays(20).withNano(0).toString(),
                "fechaFin", LocalDateTime.now().plusDays(21).withNano(0).toString());

        graphql(tokenAdmin, QUERY_DISPONIBILIDAD, Map.of("filtro", filtro))
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
        graphql(null, QUERY_DISPONIBILIDAD, Map.of("filtro", filtro))
                .andExpect(jsonPath("$.errors[0].extensions.classification").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Disponibilidad informa el error de un filtro inválido")
    void disponibilidadFiltroInvalido() throws Exception {
        Map<String, Object> filtro = Map.of(
                "fechaInicio", LocalDateTime.now().plusDays(22).withNano(0).toString(),
                "fechaFin", LocalDateTime.now().plusDays(21).withNano(0).toString());

        graphql(tokenAna, QUERY_DISPONIBILIDAD, Map.of("filtro", filtro))
                .andExpect(jsonPath("$.errors[0].message").value("fechaFin debe ser posterior a fechaInicio"));
    }

    // --- REST de reservas ---

    @Test
    @DisplayName("El alta de reserva se hace a nombre del usuario del token, aunque el body traiga otro clienteId")
    void altaUsaElClienteDelToken() throws Exception {
        String body = """
                {"clienteId": %d, "vehiculoId": %d, "fechaInicio": "%s", "fechaFin": "%s"}"""
                .formatted(ana.getId(), vehiculo.getId(),
                        LocalDateTime.now().plusDays(20).withNano(0),
                        LocalDateTime.now().plusDays(22).withNano(0));

        mockMvc.perform(post("/api/reservas")
                        .header("Authorization", "Bearer " + tokenBeto)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value(beto.getId()))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("El conflicto de disponibilidad responde 409 con el mensaje")
    void altaEnPeriodoOcupado() throws Exception {
        String body = """
                {"vehiculoId": %d, "fechaInicio": "%s", "fechaFin": "%s"}"""
                .formatted(vehiculo.getId(),
                        LocalDateTime.now().plusDays(60).plusHours(2).withNano(0),
                        LocalDateTime.now().plusDays(61).withNano(0));

        mockMvc.perform(post("/api/reservas")
                        .header("Authorization", "Bearer " + tokenBeto)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Un ADMINISTRADOR no puede crear ni cancelar reservas")
    void adminNoReserva() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reservas/" + reservaDeAna.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un cliente no puede cancelar la reserva de otro, pero sí la propia")
    void cancelarSoloLasPropias() throws Exception {
        mockMvc.perform(delete("/api/reservas/" + reservaDeAna.getId())
                        .header("Authorization", "Bearer " + tokenBeto))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No podés cancelar reservas de otro cliente"));

        mockMvc.perform(delete("/api/reservas/" + reservaDeAna.getId())
                        .header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    // --- Baja de vehículos con reservas ---

    @Test
    @DisplayName("La baja de un vehículo con reservas se permite y no toca las reservas existentes; solo el ADMINISTRADOR puede")
    void bajaDeVehiculoConReservas() throws Exception {
        String url = "/api/vehiculos/" + vehiculo.getId();

        mockMvc.perform(delete(url).header("Authorization", "Bearer " + tokenAna))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(url).header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(vehiculos.findById(vehiculo.getId()).orElseThrow().getActivo());
        org.junit.jupiter.api.Assertions.assertEquals(EstadoReserva.CONFIRMADA,
                reservas.findById(reservaDeAna.getId()).orElseThrow().getEstado());
    }

    @Test
    @DisplayName("Un vehículo dado de baja no admite reservas nuevas")
    void vehiculoInactivoNoSeReserva() throws Exception {
        mockMvc.perform(delete("/api/vehiculos/" + vehiculo.getId()).header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());

        String body = """
                {"vehiculoId": %d, "fechaInicio": "%s", "fechaFin": "%s"}"""
                .formatted(vehiculo.getId(),
                        LocalDateTime.now().plusDays(20).withNano(0),
                        LocalDateTime.now().plusDays(22).withNano(0));

        mockMvc.perform(post("/api/reservas").header("Authorization", "Bearer " + tokenBeto)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("El ADMINISTRADOR puede consultar las reservas confirmadas vigentes de un vehículo (aviso previo a la baja)")
    void reservasVigentesDeUnVehiculo() throws Exception {
        String query = """
                query($filtro: ReservaFiltro) { reservas(filtro: $filtro) { id patente estado } }""";

        Map<String, Object> conReservas = Map.of("filtro", Map.of(
                "vehiculoId", String.valueOf(vehiculo.getId()),
                "estado", "CONFIRMADA",
                "fechaDesde", LocalDateTime.now().withNano(0).toString()));
        graphql(tokenAdmin, query, conReservas)
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.reservas.length()").value(1))
                .andExpect(jsonPath("$.data.reservas[0].patente").value("AAA111"));

        // pasada la fecha de fin de la reserva ya no hay nada vigente
        Map<String, Object> sinReservas = Map.of("filtro", Map.of(
                "vehiculoId", String.valueOf(vehiculo.getId()),
                "estado", "CONFIRMADA",
                "fechaDesde", LocalDateTime.now().plusDays(90).withNano(0).toString()));
        graphql(tokenAdmin, query, sinReservas)
                .andExpect(jsonPath("$.data.reservas.length()").value(0));
    }

    // --- utilidades ---

    private Cliente cliente(String email, String documento, String nombre) {
        Usuario usuario = usuarios.save(new Usuario(email, "x", "CLIENTE"));
        return clientes.save(new Cliente(usuario, documento, nombre, "Test", null, null));
    }

    private ResultActions graphql(String token, String query, Map<String, Object> variables) throws Exception {
        var request = post("/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("query", query, "variables", variables)));
        if (token != null) request.header("Authorization", "Bearer " + token);

        ResultActions actions = mockMvc.perform(request);
        MvcResult result = actions.andReturn();
        return result.getRequest().isAsyncStarted() ? mockMvc.perform(asyncDispatch(result)) : actions;
    }
}
