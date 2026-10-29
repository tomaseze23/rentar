import { Router } from 'express';
import { clientes, vehiculos } from '../grpc/clients';
import { requerirRol } from '../auth';
import { HttpError } from '../errors';
import { asyncHandler, idNumerico } from '../http';
import { incorporacionDto } from '../mappers';
import { validarVehiculo } from '../validators';


// Incorporación de vehículos externos (Hito 3). El router se monta detrás de `autenticar` y
// permite CLIENTE y ADMINISTRADOR; cada endpoint acota el rol que corresponde.
export const vehiculosExternosRouter = Router();

// POST /api/vehiculos-externos -> 202 Accepted
// Un cliente propone incorporar un vehículo propio. La respuesta es inmediata con la solicitud
// en PENDIENTE: las validaciones (patente y titularidad) se resuelven después, por eventos.
// El cliente sale SIEMPRE del token, nunca del body.
vehiculosExternosRouter.post(
    '/',
    requerirRol('CLIENTE'),
    asyncHandler(async (req, res) => {
        const d = validarVehiculo(req.body, {patenteObligatoria: true });
        const clienteId = req.usuario?.clienteId; 
        if (!clienteId) throw new HttpError(403, 'El usuario autenticado no tiene un cliente asociado');        
        
        //Customer: ¿el cliente existe y está activo?
        const c = await clientes.verificarActivo({ id: clienteId });
        if (!c.existe) throw new HttpError(404, 'No existe un cliente asociado al usuario autenticado');
        if (!c.activo) throw new HttpError(422, 'El cliente no se encuentra activo para incorporar vehículos');

        //Vehicle: guarda la solicitud (PENDIENTE) y publica el evento. No espera las validaciones.
        const solicitud = await vehiculos.incorporarExterno({
            clienteId,
            patente: d.patente!,
            marca: d.marca,
            modelo: d.modelo,
            anio: d.anio,
            color: d.color,
            tipoVehiculo: d.tipoVehiculo,
            precioDiario: d.precioDiario,
        });
        res.status(202).json(incorporacionDto(solicitud));
    }),
);

// GET /api/vehiculos-externos
//   CLIENTE: solo sus solicitudes.
//   ADMINISTRADOR: todas; opcionalmente ?clienteId=<id> para filtrar por cliente.
vehiculosExternosRouter.get(
    '/',
    asyncHandler(async (req, res) => {
        let clienteId: string | undefined;
        if (req.usuario?.rol === 'CLIENTE') {
            clienteId = req.usuario.clienteId;
            if(!clienteId) throw new HttpError(403, 'El usuario autenticado no tiene un cliente asociado');
        } else if (req.query.clienteId !== undefined){
            clienteId = idNumerico(req.query.clienteId);
        }
        const r = await vehiculos.consultarSolicitudes({ clienteId });
        res.json(r.solicitudes.map(incorporacionDto));
    }),    
);