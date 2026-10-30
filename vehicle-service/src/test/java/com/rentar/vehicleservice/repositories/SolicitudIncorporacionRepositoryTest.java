package com.rentar.vehicleservice.repositories;

import com.rentar.vehicleservice.entities.EstadoSolicitudIncorporacion;
import com.rentar.vehicleservice.entities.ResultadoValidacion;
import com.rentar.vehicleservice.entities.SolicitudIncorporacion;
import com.rentar.vehicleservice.entities.TipoVehiculo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Igual que VehiculoRepositoryTest: la entidad tiene que coincidir con database/02_vehiculos.sql (ddl-auto=validate). */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:solicitudes;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../database/02_vehiculos.sql",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SolicitudIncorporacionRepositoryTest {

    @Autowired
    private SolicitudIncorporacionRepository repository;

    private static SolicitudIncorporacion solicitud(long clienteId, String patente, EstadoSolicitudIncorporacion estado) {
        SolicitudIncorporacion s = new SolicitudIncorporacion();
        s.setClienteId(clienteId);
        s.setPatente(patente);
        s.setMarca("Fiat");
        s.setModelo("Cronos");
        s.setAnio("2022");
        s.setTipoVehiculo(TipoVehiculo.SEDAN);
        s.setPrecioDiario(30000.5);
        s.setEstado(estado);
        s.setValidacionPatente(ResultadoValidacion.PENDIENTE);
        s.setValidacionTitularidad(ResultadoValidacion.PENDIENTE);
        s.setCompletadaPublicada(false);
        s.setFechaSolicitud(LocalDateTime.now());
        return s;
    }

    @Test
    void laEntidadCoincideConElEsquemaYFiltraPorClienteYEstado() {
        SolicitudIncorporacion a = repository.saveAndFlush(solicitud(1L, "AA100AA", EstadoSolicitudIncorporacion.PENDIENTE));
        SolicitudIncorporacion b = repository.saveAndFlush(solicitud(1L, "AA200AA", EstadoSolicitudIncorporacion.RECHAZADA));
        repository.saveAndFlush(solicitud(2L, "AB300CD", EstadoSolicitudIncorporacion.PENDIENTE));

        assertEquals(List.of(b.getId(), a.getId()),
                repository.findByClienteIdOrderByIdDesc(1L).stream().map(SolicitudIncorporacion::getId).toList());
        assertEquals(3, repository.findAllByOrderByIdDesc().size());
        assertTrue(repository.existsByPatenteAndEstado("AA100AA", EstadoSolicitudIncorporacion.PENDIENTE));
        assertFalse(repository.existsByPatenteAndEstado("AA200AA", EstadoSolicitudIncorporacion.PENDIENTE));
        assertEquals(30000.5, repository.buscarParaActualizar(a.getId()).orElseThrow().getPrecioDiario());
    }
}
