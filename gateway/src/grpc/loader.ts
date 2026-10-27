import path from 'node:path';
import * as grpc from '@grpc/grpc-js';
import * as protoLoader from '@grpc/proto-loader';
import type { ProtoGrpcType as VehiculoProto } from '../generated/vehiculo';
import type { ProtoGrpcType as ClienteProto } from '../generated/cliente';
import type { ProtoGrpcType as AlquilerProto } from '../generated/alquiler';

export const PROTO_DIR = path.resolve(__dirname, '../../../proto');

const definition = protoLoader.loadSync(
    [
        'rentar/comun/v1/comun.proto',
        'rentar/vehiculo/v1/vehiculo.proto',
        'rentar/cliente/v1/cliente.proto',
        'rentar/alquiler/v1/alquiler.proto',
    ].map((f) => path.join(PROTO_DIR, f)),
    {
        includeDirs: [PROTO_DIR],
        keepCase: false, // snake_case del .proto -> camelCase
        longs: String, // los int64 llegan como string
        enums: String, // los enums llegan como "TIPO_VEHICULO_SEDAN", "ESTADO_RESERVA_CONFIRMADA"...
        defaults: true,
        oneofs: true,
    },
);

export const proto = grpc.loadPackageDefinition(definition) as unknown as VehiculoProto &
    ClienteProto &
    AlquilerProto;