package com.rentar.vehicleservice.repositories;

import com.rentar.vehicleservice.entities.EstadoVehiculo;
import com.rentar.vehicleservice.entities.TipoVehiculo;
import com.rentar.vehicleservice.entities.Vehiculo;
import com.rentar.vehicleservice.entities.VerificacionVehiculo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Levanta JPA sobre H2 (modo PostgreSQL) con el mismo script que la base real
 * y ddl-auto=validate: si la entidad no coincide con database/02_vehiculos.sql
 * (como pasaba con precio_diario), este test falla.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vehiculos;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=file:../database/02_vehiculos.sql",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VehiculoRepositoryTest {

    @Autowired
    private VehiculoRepository repository;

    @Test
    void laEntidadCoincideConElEsquemaYGuardaElPrecio() {
        Vehiculo v = new Vehiculo(null, "AB123CD", "Fiat", "Cronos", "2024", null,
                TipoVehiculo.SEDAN, 15000.5, EstadoVehiculo.DISPONIBLE, true, VerificacionVehiculo.VALIDADO);

        Vehiculo guardado = repository.saveAndFlush(v);

        assertEquals(15000.5, repository.findById(guardado.getId()).orElseThrow().getPrecioDiario());
        assertEquals(1, repository.findByActivo(true).size());
    }
}
