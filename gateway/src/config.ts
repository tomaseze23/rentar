export const config = {
  port: Number(process.env.PORT ?? 8080),
  jwtSecret: process.env.JWT_SECRET ?? '',
  vehiculoAddr: process.env.VEHICULO_ADDR ?? 'localhost:9091',
  clienteAddr: process.env.CLIENTE_ADDR ?? 'localhost:9092',
  alquilerAddr: process.env.ALQUILER_ADDR ?? 'localhost:9093',
  grpcTimeoutMs: Number(process.env.GRPC_TIMEOUT_MS ?? 5000),
};