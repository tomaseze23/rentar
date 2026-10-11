# Customer Service (gRPC)

Servicio responsable del dominio **clientes** de Rentar (Hito 2). Implementa el contrato
[`../proto/cliente.proto`](../proto/cliente.proto) (`rentar.cliente.v1.ServicioClientes`)
y es el único dueño de la base `rentar_clientes`.

- **Lenguaje:** Java 21 + Spring Boot 4.1 (Spring gRPC). El API Gateway está en Node.
- **Puerto gRPC:** `9091` (configurable con `GRPC_PORT`).
- **Base de datos:** PostgreSQL `rentar_clientes`, creada con los scripts de [`../database`](../database).

## Operaciones

| RPC | Para qué |
|---|---|
| `ConsultarClientes` | Consultar clientes (activos, o todos con `incluir_inactivos`) |
| `ConsultarCliente` | Consultar un cliente específico |
| `VerificarClienteActivo` | Verificar existencia y si está activo (lo usa el Gateway antes de reservar) |
| `CrearCliente` / `ActualizarCliente` | ABM con las validaciones del dominio |
| `DarDeBajaCliente` / `ReactivarCliente` | Baja lógica y reactivación |
| `ValidarCredencialesLogin` | Valida email/contraseña y devuelve rol y `cliente_id`. El JWT lo firma el Gateway |

Errores → códigos gRPC: `INVALID_ARGUMENT`, `NOT_FOUND`, `ALREADY_EXISTS`,
`FAILED_PRECONDITION`. Credenciales incorrectas no son un error: responden
`credenciales_validas = false`.

## Estructura

```
src/main/java/com/rentar/customer/
├── grpc/          CustomerGrpcService (implementa el .proto), ClienteMapper, GrpcErrorHandler
├── services/      ClienteService y AuthService: reglas de negocio (migradas del Hito 1)
├── entities/      Cliente, Usuario
├── repositories/  Spring Data JPA
├── dtos/          Objetos del dominio
├── exceptions/    Excepciones del dominio
└── config/        BCrypt y AdminSeeder (admin@rentar.com / admin1234)
```

Las clases Java del contrato (paquete `com.rentar.proto.cliente`) **no se versionan**:
el `protobuf-maven-plugin` las genera en `target/` desde `../proto/cliente.proto` al compilar.

## Cómo correrlo

```bash
# 1. Crear la base y las tablas (una sola vez), desde la carpeta database/
psql -U postgres -f 00_crear_bases.sql
psql -U postgres -d rentar_clientes -f 01_clientes.sql

# 2. Levantar el servicio
cd customer-service
DB_USER=postgres DB_PASSWORD=postgres mvn spring-boot:run
```

Variables de entorno: `DB_USER`, `DB_PASSWORD`, `DB_URL` (opcional) y `GRPC_PORT` (opcional).
Hibernate corre con `ddl-auto=validate`: no crea tablas, sólo verifica que coincidan con el script.

## Con Docker

Desde la raíz del repo:

```bash
docker compose up --build
```

Levanta PostgreSQL (crea las tres bases con los scripts de `database/` la primera vez) y el
Customer Service en `localhost:9091`. `docker compose down -v` borra las bases para empezar de cero.

La imagen se construye desde la raíz porque necesita `proto/cliente.proto`:

```bash
docker build -f customer-service/Dockerfile -t rentar/customer-service .
```

## Probarlo a mano

El servicio tiene *reflection* habilitado, así que se puede usar Postman (New → gRPC →
`localhost:9091`) o [grpcurl](https://github.com/fullstorydev/grpcurl) sin cargar el `.proto`:

```bash
grpcurl -plaintext localhost:9091 list rentar.cliente.v1.ServicioClientes

grpcurl -plaintext -d '{"email":"juan@mail.com","password":"secreto123","documento":"30123456",
  "nombre":"Juan","apellido":"Pérez","fecha_nacimiento":"1990-05-20"}' \
  localhost:9091 rentar.cliente.v1.ServicioClientes/CrearCliente

grpcurl -plaintext -d '{"id": 1}' localhost:9091 rentar.cliente.v1.ServicioClientes/VerificarClienteActivo
```

## Tests

```bash
mvn test
```

- `ClienteServiceTest` y `AuthServiceTest`: reglas de negocio con Mockito (portados del Hito 1).
- `CustomerGrpcServiceIntegrationTest`: levanta el servicio completo y lo invoca con un stub
  gRPC real. Usa H2 en modo PostgreSQL con el script `database/01_clientes.sql`, así que también
  verifica que las entidades coincidan con el esquema.
