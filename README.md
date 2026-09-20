# Rentar

API para el sistema de alquiler de vehículos, desarrollada como Trabajo Práctico de la materia **Desarrollo de Software en Sistemas Distribuidos**.

Expone una API **REST** documentada con Swagger/OpenAPI y una API **GraphQL**, sobre una base de datos PostgreSQL, con autenticación y autorización mediante **Spring Security + JWT**.

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

Antes de ejecutar, definir esas tres variables. En IntelliJ se configuran en la *Run Configuration* de la aplicación (sección *Environment variables*).

> `JWT_SECRET` debe tener **al menos 32 caracteres**. El algoritmo de firma (HMAC-SHA256) exige una clave de 256 bits como mínimo; si el secreto es más corto, la aplicación no arranca.

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

Al arrancar por primera vez, se **siembra automáticamente un usuario administrador.**

## Autenticación y roles

La API está protegida: salvo el login y la documentación, **todos los endpoints requieren un token JWT**.

### Cómo obtener el token

1. Hacer login en `POST /api/auth/login` con email y contraseña:
   ```json
   { "email": "admin@rentar.com", "password": "admin1234" }
   ```
2. La respuesta devuelve un token:
   ```json
   { "token": "eyJhbGciOiJIUzI1NiJ9..." }
   ```
3. Enviar ese token en el header de las demás llamadas:
   ```
   Authorization: Bearer <token>
   ```

En Swagger UI se usa el botón **Authorize** 🔒 (arriba a la derecha): se pega el token una vez y se envía automáticamente en todas las llamadas. En GraphiQL, se agrega manualmente en la pestaña **Headers**:
```json
{ "Authorization": "Bearer token" }
```

### Usuario administrador de desarrollo

Se crea solo al levantar la aplicación (no hace falta cargarlo a mano):

| Email               | Contraseña  | Rol            |
|---------------------|-------------|----------------|
| `admin@rentar.com`  | `admin1234` | ADMINISTRADOR  |

> Es un admin de desarrollo, pensado para poder probar los endpoints de administrador. Su contraseña se guarda **encriptada** (BCrypt), igual que la del resto de los usuarios.

### Permisos por rol

| Rol             | Puede acceder a                                              |
|-----------------|-------------------------------------------------------------|
| `ADMINISTRADOR` | ABM de vehículos, ABM de clientes                           |
| `CLIENTE`       | Alta y cancelación de reservas, consulta e historial        |

Los clientes se crean desde el ABM de clientes (rol ADMINISTRADOR); al crear un cliente se genera también su usuario con rol CLIENTE.

## Endpoints

Con la aplicación corriendo:

| Recurso            | URL                                            | Acceso        |
|--------------------|------------------------------------------------|---------------|
| Login              | http://localhost:8080/api/auth/login           | Público       |
| Swagger UI         | http://localhost:8080/swagger-ui/index.html    | Público       |
| OpenAPI (JSON)     | http://localhost:8080/v3/api-docs              | Público       |
| GraphiQL           | http://localhost:8080/graphiql                 | Público (*)   |
| ABM de vehículos   | http://localhost:8080/api/vehiculos            | ADMINISTRADOR |
| ABM de clientes    | http://localhost:8080/api/clientes             | ADMINISTRADOR |
| Reservas           | http://localhost:8080/api/reservas             | CLIENTE       |

(*) GraphiQL como interfaz es pública, pero las queries de reservas/historial requieren token con rol CLIENTE (se envía en la pestaña Headers).

## Funcionalidades (Hito 1)

- **Vehículos** — ABM completo (REST).
- **Consulta de disponibilidad** — GraphQL.
- **Clientes** — ABM completo (REST) + operaciones por GraphQL.
- **Reservas** — alta con validaciones y cálculo de importe (REST).
- **Cancelación de reservas** — baja lógica, solo si el período no comenzó (REST).
- **Consulta de reservas e historial de alquileres** — GraphQL, filtrado por el usuario autenticado.
- **Seguridad** — login con JWT, encriptación de contraseñas y autorización por rol.

## Estructura del proyecto

```
src/main/java/com/rentar/rentar/
├── RentarApplication.java
├── config/
│   ├── SecurityConfig.java        # Cadena de seguridad, protección por rol
│   ├── PasswordEncoderConfig.java # Bean de BCrypt
│   ├── OpenApiConfig.java         # Config de Swagger (botón Authorize)
│   └── AdminSeeder.java           # Siembra del admin al arrancar
├── controllers/                   # Controladores REST (auth, clientes, vehículos, reservas)
├── dtos/                          # Objetos de entrada/salida de la API
├── entities/                      # Entidades JPA (Usuario, Cliente, Vehiculo, Reserva, enums)
├── exceptions/                    # Excepciones propias y manejadores (REST y GraphQL)
├── graphql/                       # Controladores GraphQL (clientes, consulta de reservas)
├── repositories/                  # Repositorios Spring Data JPA
├── security/                      # JwtService y JwtAuthFilter
└── services/                      # Lógica de negocio

src/main/resources/
├── application.properties
└── graphql/
    └── schema.graphqls            # Esquema GraphQL
```

## Tests

El proyecto incluye tests unitarios (Mockito) y de contexto. Para correrlos:

```bash
mvnw.cmd test
```

Los tests usan una base H2 en memoria, así que **no requieren PostgreSQL corriendo**.

## Autores

- *DanielVisaggi*
- *Marki0*
- *RonnyOI*
- *BenGonza239*
- *tomaseze23*