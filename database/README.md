# Base de datos por servicio - Rentar Hito 2

## Estructura propuesta

- Customer Service -> `rentar_clientes`
- Vehicle Service -> `rentar_vehiculos`
- Rental Service -> `rentar_alquileres`

El API Gateway no posee una base de datos de dominio y no debe acceder
directamente a estas bases.

## Archivos

- `00_crear_bases.sql`: crea las tres bases de datos.
- `01_clientes.sql`: crea `usuario` y `cliente`.
- `02_vehiculos.sql`: crea `vehiculo`.
- `03_alquileres.sql`: crea `reservas`.

## Orden de ejecución

1. Conectarse a PostgreSQL (por ejemplo a la base `postgres`) y ejecutar
   `00_crear_bases.sql`.
2. Conectarse a `rentar_clientes` y ejecutar `01_clientes.sql`.
3. Conectarse a `rentar_vehiculos` y ejecutar `02_vehiculos.sql`.
4. Conectarse a `rentar_alquileres` y ejecutar `03_alquileres.sql`.

## application.properties (si el servicio usa Spring Boot)

### Customer Service

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/rentar_clientes?sslmode=disable
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
```

### Vehicle Service

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/rentar_vehiculos?sslmode=disable
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
```

### Rental Service

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/rentar_alquileres?sslmode=disable
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
```

`validate` es recomendable una vez que el esquema se crea mediante scripts:
Hibernate verifica que las tablas sean compatibles en lugar de modificarlas
automáticamente.

## Relaciones entre servicios

`reservas.cliente_id` y `reservas.vehiculo_id` NO tienen foreign keys a
`rentar_clientes` ni `rentar_vehiculos`. Son IDs de otros dominios.

La validación de esos IDs debe hacerse a través de los servicios/gRPC y no
mediante acceso directo a las bases ajenas.
