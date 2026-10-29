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
| `POST /api/vehiculos-externos` | CLIENTE | Proponer incorporar un vehículo propio. Responde **202** con la solicitud en `PENDIENTE`; las validaciones de patente y titularidad son asíncronas (eventos) |
| `GET /api/vehiculos-externos` (`?clienteId`) | CLIENTE (las propias) / ADMINISTRADOR (todas) | Estado de las solicitudes y de cada validación |

## Incorporación de vehículos externos (Hito 3)
`POST /api/vehiculos-externos` valida el body (mismos datos que un vehículo), toma el `clienteId` del token, verifica con Clientes que esté activo y le pasa la solicitud a Vehículos (`IncorporarVehiculoExterno`), que la guarda en `PENDIENTE` y responde enseguida (**202**). El gateway no espera las validaciones: el cliente consulta el avance con `GET /api/vehiculos-externos`.

Respuesta: `{ id, clienteId, patente, marca, modelo, anio, color, tipoVehiculo, precioDiario, estado, validacionPatente, validacionTitularidad, vehiculoId }`, con `estado` en `PENDIENTE | RECHAZADA | INCORPORADA` y cada validación en `PENDIENTE | VALIDADO | RECHAZADO`. `vehiculoId` aparece cuando la solicitud queda `INCORPORADA`.

Campos nuevos: los vehículos suman `verificacion` (`PENDIENTE | VALIDADO | ROBADO`) y `propietarioClienteId`; las reservas (REST y GraphQL `ReservaConsulta`) suman `mantenimiento` (`PENDIENTE | REALIZADO | NO_REALIZADO`). Un vehículo `ROBADO` no aparece como disponible y no se puede reservar (422); con verificación `PENDIENTE` sí se puede.

Los valores se informan sin los prefijos del `.proto`: la traducción está en `src/dominio.ts`.

### Mocks del Hito 3 (`npm run mocks`)
El mock de Vehículos simula el flujo asíncrono: la solicitud nace `PENDIENTE` y a los 3 segundos se resuelve sola. Una patente que empieza con `ROB` se rechaza en la validación de patente, una que empieza con `SIN` en la de titularidad, y cualquier otra queda `INCORPORADA` (y se crea el vehículo). El vehículo `ROB123` ya existe como `ROBADO`.

## Formatos

Errores con el formato `{ "timestamp", "status", "message" }`. Mapeo gRPC → HTTP: INVALID_ARGUMENT 400, UNAUTHENTICATED 401, PERMISSION_DENIED 403, NOT_FOUND 404, ALREADY_EXISTS y FAILED_PRECONDITION 409, UNAVAILABLE 503, DEADLINE_EXCEEDED 504. El gateway responde 422 si el cliente o el vehículo están dados de baja al reservar o si el vehículo figura como ROBADO.

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