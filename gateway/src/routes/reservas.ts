import { Router } from 'express';
import { alquileres, clientes, vehiculos } from '../grpc/clients';
import { HttpError } from '../errors';
import { asyncHandler, idNumerico } from '../http';
import { reservaDto } from '../mappers';
import { validarReserva } from '../validators';

export const reservasRouter = Router();

// El cliente sale SIEMPRE del token, nunca del body.
function clienteDelToken(req: { usuario?: { clienteId?: string } }): string {
  const id = req.usuario?.clienteId;
  if (!id) throw new HttpError(403, 'El usuario autenticado no tiene un cliente asociado');
  return id;
}

// POST /api/reservas -> 201
reservasRouter.post(
  '/',
  asyncHandler(async (req, res) => {
    const d = validarReserva(req.body);
    const clienteId = clienteDelToken(req);

    // 1) Customer: ¿el cliente existe y está activo?
    const c = await clientes.verificarActivo({ id: clienteId });
    if (!c.existe) throw new HttpError(404, 'No existe un cliente asociado al usuario autenticado');
    if (!c.activo) throw new HttpError(422, 'El cliente no se encuentra activo para realizar reservas');


    // 2) Vehicle: ¿existe (si no, NOT_FOUND -> 404) y está activo? De acá sale el precio.
    const v = await vehiculos.consultar({ id: d.vehiculoId });
    if (!v.activo) throw new HttpError(422, 'El vehículo no se encuentra activo');
    
    // Hito 3: un vehículo ROBADO (según el registro policial) queda inhabilitado, igual que uno dado de baja.
    // Con verificación PENDIENTE sí se puede reservar: solo se inhabilita si el resultado es ROBADO.
    if (v.verificacion === 'ROBADO') throw new HttpError(422, 'El vehículo no se encuentra habilitado para alquilar');

    // 3) Rental: crea la reserva (verifica solapamiento y calcula el importe)
    const reserva = await alquileres.crearReserva({
      clienteId,
      vehiculoId: d.vehiculoId,
      periodo: { fechaInicio: d.fechaInicio, fechaFin: d.fechaFin },
      precioDiario: v.precioDiario,
    });

    res.status(201).json(reservaDto(reserva));
  }),
);

// DELETE /api/reservas/:id -> 200 con la reserva cancelada
reservasRouter.delete(
  '/:id',
  asyncHandler(async (req, res) => {
    const reserva = await alquileres.cancelarReserva({
      reservaId: idNumerico(req.params.id),
      clienteId: clienteDelToken(req),
    });
    res.json(reservaDto(reserva));
  }),
);