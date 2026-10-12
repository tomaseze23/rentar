import { Router } from 'express';
import { clientes } from '../grpc/clients';
import { asyncHandler, idNumerico } from '../http';
import { clienteDto } from '../mappers';
import { validarCliente } from '../validators';

export const clientesRouter = Router();

// GET /api/clientes?incluirInactivos=true
clientesRouter.get(
    '/',
    asyncHandler(async (req, res) => {
        const r = await clientes.consultarVarios({ incluirInactivos: req.query.incluirInactivos === 'true'});
        res.json(r.clientes.map(clienteDto));
    }),
);

// GET /api/clientes/:id
clientesRouter.get(
    '/:id',
    asyncHandler(async (req, res) => {
        res.json(clienteDto(await clientes.consultar({ id: idNumerico(req.params.id)})));
    }),
);

// POST /api/clientes -> 201 (crea también el usuario para que pueda iniciar sesión)
clientesRouter.post(
    '/',
    asyncHandler(async (req, res) => {
        const d = validarCliente(req.body, true);
        const creado = await clientes.crear({
              email: d.email!,
              password: d.password!,
              documento: d.documento!,
              nombre: d.nombre,
              apellido: d.apellido,
              telefono: d.telefono,
              fechaNacimiento: d.fechaNacimiento,
        });
        res.status(201).json(clienteDto(creado));
    }),
);

// PUT /api/clientes/:id  (email y documento no se modifican)
clientesRouter.put(
    '/:id',
    asyncHandler(async(req, res) =>{
        const d = validarCliente(req.body, false);
        const actualizado = await clientes.actualizar({
              id: idNumerico(req.params.id),
              nombre: d.nombre,
              apellido: d.apellido,
              telefono: d.telefono,
              fechaNacimiento: d.fechaNacimiento,
        });
        res.json(clienteDto(actualizado));
    }),
);

// DELETE /api/clientes/:id -> 204 (baja lógica)
clientesRouter.delete(
    '/:id',
    asyncHandler(async (req, res) => {
        await clientes.darDeBaja({ id: idNumerico(req.params.id) });
        res.status(204).send();
    }),
);

// PATCH /api/clientes/:id/reactivar
clientesRouter.patch(
    '/:id/reactivar',
    asyncHandler(async (req, res) => {
        res.json(clienteDto(await clientes.reactivar({ id: idNumerico(req.params.id) })));
    }),
);