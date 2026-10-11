import { Router } from 'express';
import { vehiculos } from '../grpc/clients';
import { HttpError } from '../errors';
import { asyncHandler } from '../http';
import { vehiculoDto } from '../mappers';
import type { TipoVehiculo } from '../generated/rentar/comun/v1/TipoVehiculo';

export const vehiculosRouter = Router();

const TIPOS = ['SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK'];

// GET /api/vehiculos?activo=true&tipo=SEDAN
vehiculosRouter.get(
    '/',
    asyncHandler(async (req, res) => {
        const { activo, tipo } = req.query;

        if (tipo !== undefined && !TIPOS.includes(String(tipo))) {
            throw new HttpError(400, `Tipo de vehículo inválido: ${tipo}`);
        }
        const respuesta = await vehiculos.consultarVarios({
            ids: [], // lista vacía = sin filtro por ids
            activo: activo === undefined ? undefined : activo === 'true',
            tipoVehiculo: tipo === undefined ? undefined : (String(tipo) as TipoVehiculo),
        });

        res.json(respuesta.vehiculos.map(vehiculoDto));
    }),
);

// GET /api/vehiculos/:id
vehiculosRouter.get(
    '/:id',
    asyncHandler(async (req, res) => {
        const id = String(req.params.id);
        if (!/^\d+$/.test(id)) throw new HttpError(400, 'El id debe ser numérico');

        const v = await vehiculos.consultar({ id });
        res.json(vehiculoDto(v));
    }),
);