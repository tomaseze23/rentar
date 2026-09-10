# Rentar

API para el sistema de alquiler de vehículos, desarrollada como Trabajo Práctico de la materia Desarollo de Software en Sistemas Distribuidos.

Expone una API **REST** documentada con Swagger/OpenAPI y una API **GraphQL**, sobre una base de datos PostgreSQL, con Spring Security preparado para autenticación por JWT.

## Tecnologías

- **Java 21**
- **Spring Boot 4.1.1**
  - Spring Web (REST)
  - Spring for GraphQL
  - Spring Data JPA (Hibernate)
  - Spring Security
- **PostgreSQL**
- **springdoc-openapi** (Swagger UI / OpenAPI 3.1)
- **JJWT** (JSON Web Tokens)
- **Maven** (con wrapper incluido)

## Requisitos previos

- JDK 21 instalado.
- PostgreSQL corriendo en `localhost:5432`.
- Una base de datos llamada `rentar` creada (por ejemplo, desde pgAdmin).

## Configuración

La aplicación **no guarda credenciales en el código**. Los datos sensibles se leen de variables de entorno:

| Variable      | Descripción                                  |
|---------------|----------------------------------------------|
| `DB_USER`     | Usuario de PostgreSQL                         |
| `DB_PASSWORD` | Contraseña de PostgreSQL                      |
| `JWT_SECRET`  | Clave secreta para firmar los tokens JWT      |

Antes de ejecutar, definir esas tres variables. En IntelliJ, por ejemplo, se configuran en la *Run Configuration* de la aplicación (sección *Environment variables*).

## Cómo ejecutar

Desde la raíz del proyecto:

**Windows:**
```bash
mvnw.cmd spring-boot:run
```

**Linux / macOS:**
```bash
./mvnw spring-boot:run
```

O directamente desde IntelliJ con el botón *Run* sobre `RentarApplication`.

La aplicación levanta en el puerto **8080**.

## Endpoints

Con la aplicación corriendo:

| Recurso           | URL                                            |
|-------------------|------------------------------------------------|
| Swagger UI        | http://localhost:8080/swagger-ui/index.html    |
| OpenAPI (JSON)    | http://localhost:8080/v3/api-docs              |
| GraphiQL          | http://localhost:8080/graphiql                 |
| Endpoint de prueba REST | http://localhost:8080/api/ping           |

> Nota: `/api/ping` requiere autenticación (devuelve 401/403) porque en `SecurityConfig` todo lo que no está en la lista pública exige estar autenticado. Las rutas de documentación y GraphQL están abiertas para el desarrollo.

## Estructura del proyecto

```
src/main/java/com/rentar/rentar/
├── RentarApplication.java        # Clase principal
├── config/
│   └── SecurityConfig.java       # Configuración de Spring Security
├── controllers/
│   └── PruebaController.java     # Controlador REST de prueba
└── graphql/                      # (código relacionado a GraphQL)

src/main/resources/
├── application.properties        # Configuración de la app
└── graphql/
    └── schema.graphqls           # Esquema GraphQL
```

