package com.rentar.rentar.services;

import com.rentar.rentar.dtos.DisponibilidadFiltro;
import com.rentar.rentar.dtos.VehiculoDisponibleResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(DisponibilidadService.class)
@TestPropertySource(properties = {
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Consulta de disponibilidad de vehículos")
class DisponibilidadServiceTest {

    @Autowired private DisponibilidadService servicio;
    @Autowired private VehiculoRepository vehiculos;
    @Autowired private ReservaRepository reservas;
    @Autowired private ClienteRepository clientes;
    @Autowired private UsuarioRepository usuarios;

    // Fechas fijas (sin nanosegundos) para poder probar los bordes exactos del período.
    private final LocalDateTime base = LocalDateTime.now().plusDays(10).withNano(0);
    private final LocalDateTime reservaInicio = base;
    private final LocalDateTime reservaFin = base.plusDays(3);

    @BeforeEach
    void datos() {
        Usuario usuario = usuarios.save(new Usuario("cliente@mail.com", "x", "CLIENTE"));
        Cliente cliente = clientes.save(new Cliente(usuario, "30111222", "Ana", "Gómez", null, null));

        Vehiculo corolla = vehiculos.save(vehiculo("AAA111", "Toyota", "Corolla", TipoVehiculo.SEDAN, 15000.0, true));
        Vehiculo taos = vehiculos.save(vehiculo("BBB222", "Volkswagen", "Taos", TipoVehiculo.SUV, 58000.0, true));
        vehiculos.save(vehiculo("CCC333", "Ford", "Ranger", TipoVehiculo.PICKUP, 40000.0, false));

        reservas.save(reserva(cliente, corolla, EstadoReserva.CONFIRMADA));
        reservas.save(reserva(cliente, taos, EstadoReserva.CANCELADA));
    }

    @Test
    @DisplayName("Excluye vehículos con reserva confirmada superpuesta, pero no los que solo tienen una cancelada")
    void excluyeSuperpuestosYConsideraCanceladasComoLibres() {
        List<String> patentes = patentes(filtro(base.plusDays(1), base.plusDays(2)));

        assertEquals(List.of("BBB222"), patentes); // Corolla ocupado; Taos libre (reserva cancelada); Ranger inactivo
    }

    @Test
    @DisplayName("Sin superposición devuelve todos los vehículos activos")
    void periodoLibre() {
        List<String> patentes = patentes(filtro(base.plusDays(20), base.plusDays(21)));

        assertEquals(List.of("AAA111", "BBB222"), patentes.stream().sorted().toList());
    }

    @Test
    @DisplayName("Un período que solo toca el borde de otra reserva no se considera superpuesto")
    void bordesExactosNoSuperponen() {
        assertTrue(patentes(filtro(reservaFin, reservaFin.plusDays(1))).contains("AAA111"));
        assertTrue(patentes(filtro(reservaInicio.minusDays(1), reservaInicio)).contains("AAA111"));
    }

    @Test
    @DisplayName("Un período que solapa apenas un instante con la reserva la excluye")
    void superposicionParcialExcluye() {
        assertTrue(!patentes(filtro(reservaFin.minusHours(1), reservaFin.plusDays(1))).contains("AAA111"));
        assertTrue(!patentes(filtro(reservaInicio.minusDays(1), reservaInicio.plusHours(1))).contains("AAA111"));
    }

    @Test
    @DisplayName("Aplica los filtros opcionales de tipo, marca, modelo y rango de precio")
    void filtrosOpcionales() {
        DisponibilidadFiltro f = filtro(base.plusDays(20), base.plusDays(21));

        f.setTipoVehiculo(TipoVehiculo.SUV);
        assertEquals(List.of("BBB222"), patentes(f));

        f = filtro(base.plusDays(20), base.plusDays(21));
        f.setMarca("TOY"); // parcial y sin distinguir mayúsculas
        assertEquals(List.of("AAA111"), patentes(f));

        f = filtro(base.plusDays(20), base.plusDays(21));
        f.setModelo("aos");
        assertEquals(List.of("BBB222"), patentes(f));

        f = filtro(base.plusDays(20), base.plusDays(21));
        f.setPrecioMax(20000.0);
        assertEquals(List.of("AAA111"), patentes(f));

        f = filtro(base.plusDays(20), base.plusDays(21));
        f.setPrecioMin(20000.0);
        assertEquals(List.of("BBB222"), patentes(f));
    }

    @Test
    @DisplayName("Devuelve los datos que pide el enunciado")
    void devuelveLosCamposDelEnunciado() {
        DisponibilidadFiltro f = filtro(base.plusDays(20), base.plusDays(21));
        f.setMarca("toyota");

        VehiculoDisponibleResponse v = servicio.buscar(f).get(0);

        assertEquals("AAA111", v.getPatente());
        assertEquals("Toyota", v.getMarca());
        assertEquals("Corolla", v.getModelo());
        assertEquals("2022", v.getAnio());
        assertEquals("Gris", v.getColor());
        assertEquals(TipoVehiculo.SEDAN, v.getTipoVehiculo());
        assertEquals(15000.0, v.getPrecioDiario());
    }

    @Test
    @DisplayName("Rechaza filtros inválidos")
    void validaciones() {
        assertThrows(IllegalArgumentException.class, () -> servicio.buscar(null));

        DisponibilidadFiltro sinFechas = new DisponibilidadFiltro();
        assertThrows(IllegalArgumentException.class, () -> servicio.buscar(sinFechas));

        assertThrows(IllegalArgumentException.class, () -> servicio.buscar(filtro(base.plusDays(2), base.plusDays(1))));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.buscar(filtro(LocalDateTime.now().minusDays(1), base)));

        DisponibilidadFiltro precios = filtro(base.plusDays(20), base.plusDays(21));
        precios.setPrecioMin(500.0);
        precios.setPrecioMax(100.0);
        assertThrows(IllegalArgumentException.class, () -> servicio.buscar(precios));

        DisponibilidadFiltro formato = new DisponibilidadFiltro();
        formato.setFechaInicio("mañana");
        formato.setFechaFin("pasado");
        assertThrows(IllegalArgumentException.class, () -> servicio.buscar(formato));
    }

    private List<String> patentes(DisponibilidadFiltro filtro) {
        return servicio.buscar(filtro).stream().map(VehiculoDisponibleResponse::getPatente).toList();
    }

    private DisponibilidadFiltro filtro(LocalDateTime inicio, LocalDateTime fin) {
        DisponibilidadFiltro f = new DisponibilidadFiltro();
        f.setFechaInicio(inicio.toString());
        f.setFechaFin(fin.toString());
        return f;
    }

    private Vehiculo vehiculo(String patente, String marca, String modelo, TipoVehiculo tipo, double precio, boolean activo) {
        Vehiculo v = new Vehiculo();
        v.setPatente(patente);
        v.setMarca(marca);
        v.setModelo(modelo);
        v.setAnio("2022");
        v.setColor("Gris");
        v.setTipoVehiculo(tipo);
        v.setPrecioDiario(precio);
        v.setActivo(activo);
        return v;
    }

    private Reserva reserva(Cliente cliente, Vehiculo vehiculo, EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setCliente(cliente);
        r.setVehiculo(vehiculo);
        r.setFechaInicio(reservaInicio);
        r.setFechaFin(reservaFin);
        r.setPrecioDiario(BigDecimal.valueOf(vehiculo.getPrecioDiario()));
        r.setImporteTotal(BigDecimal.valueOf(vehiculo.getPrecioDiario() * 3));
        r.setEstado(estado);
        return r;
    }
}
