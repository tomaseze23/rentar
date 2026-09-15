package com.rentar.rentar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Test de humo que solo verifica que el contexto de Spring levanta.
 * Usa H2 en memoria en vez de Postgres para no depender de una base
 * real ni de las variables de entorno DB_USER/DB_PASSWORD/JWT_SECRET.
 */
@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:rentar-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=update",
		"jwt.secret=test-secret-not-for-production"
})
class RentarApplicationTests {

	@Test
	void contextLoads() {
	}

}