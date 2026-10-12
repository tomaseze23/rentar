# Gateway Rentar

API Gateway del TP Rentar (Hito 2). Expone **REST** y **GraphQL** hacia el frontend y habla **gRPC** con los tres microservicios (Vehículos, Clientes, Alquileres). Maneja la autenticación (JWT) y la orquestación entre servicios.

## Requisitos
- Node.js 20 o superior
- Los `.proto` compartidos en `/proto` (se generan los tipos TypeScript al instalar)

## Instalación y arranque
```bash
cd gateway
npm install        # también genera los tipos en src/generated (postinstall)
npm run dev        # desarrollo, recarga automática
npm run build && npm start   # producción
```
Para desarrollar sin los servicios reales: `npm run mocks` levanta mocks gRPC de los tres servicios en los puertos 9091, 9092 y 9093.

## Configuración (variables de entorno)
| Variable | Default | Descripción |
|---|---|---|
| `PORT` | 8080 | Puerto HTTP del gateway |
| `JWT_SECRET` | secreto de desarrollo | Secreto para firmar los tokens. **Definirlo en producción** |
| `VEHICULO_ADDR` | localhost:9091 | Servicio de Vehículos |
| `CLIENTE_ADDR` | localhost:9092 | Servicio de Clientes |
| `ALQUILER_ADDR` | localhost:9093 | Servicio de Alquileres |
| `GRPC_TIMEOUT_MS` | 5000 | Deadline de cada llamada gRPC |
| `CORS_ORIGIN` | * | Origen permitido para el navegador |

## Autenticación
`POST /api/auth/login` con `{ "email", "password" }` devuelve un JWT (HS256, 24 h) con el rol (`ADMINISTRADOR` o `CLIENTE`). Se envía en cada pedido como `Authorization: Bearer <token>`.
- 401: falta el token o es inválido/vencido.
- 403: el rol no tiene permiso para esa operación.

## REST
| Método y ruta | Rol | Descripción |
|---|---|---|
| `POST /api/auth/login` | público | Login |
| `GET /api/vehiculos` (`?activo`, `?tipo`) | ADMINISTRADOR | Lista vehículos |
| `GET /api/vehiculos/:id` | ADMINISTRADOR | Detalle |
| `POST /api/vehiculos` | ADMINISTRADOR | Alta (201) |
| `PUT /api/vehiculos/:id` | ADMINISTRADOR | Modificación |
| `DELETE /api/vehiculos/:id` | ADMINISTRADOR | Baja lógica (204) |
| `PATCH /api/vehiculos/:id/reactivar` | ADMINISTRADOR | Reactivar |
| `/api/clientes` (mismas operaciones, `?incluirInactivos`) | ADMINISTRADOR | ABM de clientes |
| `POST /api/reservas` | CLIENTE | Crear reserva (201) |
| `DELETE /api/reservas/:id` | CLIENTE | Cancelar reserva propia |

Errores con el formato `{ "timestamp", "status", "message" }`. Mapeo gRPC → HTTP: INVALID_ARGUMENT 400, UNAUTHENTICATED 401, PERMISSION_DENIED 403, NOT_FOUND 404, ALREADY_EXISTS y FAILED_PRECONDITION 409, UNAVAILABLE 503, DEADLINE_EXCEEDED 504. El gateway responde 422 si el cliente o el vehículo están dados de baja al reservar.

## GraphQL
Endpoint `POST /graphql`, con el mismo schema del Hito 1 (`src/graphql/schema.graphqls`). El token es opcional a nivel HTTP; cada operación valida el rol. Los errores se informan en `errors[].extensions.classification`: `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `BAD_REQUEST`, `INTERNAL_ERROR`.

| Operación | Rol |
|---|---|
| `clientes`, `cliente`, `crearCliente`, `actualizarCliente`, `eliminarCliente`, `reactivarCliente` | ADMINISTRADOR |
| `vehiculosDisponibles` | CLIENTE |
| `reservas` | CLIENTE (solo las propias) / ADMINISTRADOR (todas) |
| `historialAlquileres` | CLIENTE |

## Orquestación de una reserva (`POST /api/reservas`)
1. Valida el body y toma el `clienteId` del token.
2. Clientes: `VerificarClienteActivo`.
3. Vehículos: `ConsultarVehiculo` (precio y que esté activo).
4. Alquileres: `CrearReserva` (valida superposición y calcula el importe).

## Formatos
Fechas con hora sin zona: `yyyy-MM-ddTHH:mm:ss`. Facturación: `ceil(duración / 24 h)` días.