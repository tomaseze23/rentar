import { Router } from 'express';
import { vehiculos } from '../grpc/clients';
import { HttpError } from '../errors';
import { asyncHandler, idNumerico } from '../http';
import { vehiculoDto } from '../mappers';
import type { TipoVehiculo } from '../generated/rentar/comun/v1/TipoVehiculo';
import { validarVehiculo } from '../validators';

export const vehiculosRouter = Router();

const TIPOS = ['SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK'];

const normalizarPatente = (p: string) => p.replace(/\s/g, '').toUpperCase();

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
        const v = await vehiculos.consultar({ id: idNumerico(req.params.id) });
        res.json(vehiculoDto(v));
    }),
);

// POST /api/vehiculos  -> 201
vehiculosRouter.post(
    '/',
    asyncHandler(async (req, res) => {
        const d = validarVehiculo(req.body, { patenteObligatoria: true });
        const creado = await vehiculos.crear({
            patente: d.patente!,
            marca: d.marca,
            modelo: d.modelo,
            anio: d.anio,
            color: d.color,
            tipoVehiculo: d.tipoVehiculo,
            precioDiario: d.precioDiario,
        });
        res.status(201).json(vehiculoDto(creado));
    }),
);

// PUT /api/vehiculos/:id  -> 200
vehiculosRouter.put(
    '/:id',
    asyncHandler(async (req, res) => {
        const id = idNumerico(req.params.id);
        const d = validarVehiculo(req.body, {patenteObligatoria: false});

        // la patente no se puede modificar: si viene tiene que coincidir con la registrada.
        if (d.patente) {
          const actual = await vehiculos.consultar({ id });
          if (normalizarPatente(actual.patente) !== normalizarPatente(d.patente)) {
            throw new HttpError(400, 'La patente no se puede modificar');
          }
        }
        const actualizado = await vehiculos.actualizar({
             id,
             marca: d.marca,
             modelo: d.modelo,
             anio: d.anio,
             color: d.color,
             tipoVehiculo: d.tipoVehiculo,
             precioDiario: d.precioDiario,
        });
    res.json(vehiculoDto(actualizado));
    }),
);

// DELETE /api/vehiculos/:id  -> 204 (baja lógica)
vehiculosRouter.delete(
  '/:id',
  asyncHandler(async (req, res) => {
    await vehiculos.darDeBaja({ id: idNumerico(req.params.id) });
    res.status(204).send();
  }),
);

// PATCH /api/vehiculos/:id/reactivar  -> 200
vehiculosRouter.patch(
  '/:id/reactivar',
  asyncHandler(async (req, res) => {
    const reactivado = await vehiculos.reactivar({ id: idNumerico(req.params.id) });
    res.json(vehiculoDto(reactivado));
  }),
);