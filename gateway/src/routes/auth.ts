import { Router } from 'express';
import { clientes } from '../grpc/clients';
import { HttpError } from '../errors';
import { asyncHandler } from '../http';
import { firmarToken, Rol } from '../auth';

export const authRouter = Router();

// POST /api/auth/login   body: { email, password }   respuesta: { token }
authRouter.post(
    '/login',
    asyncHandler(async (req, res) => {
        const { email, password } = req.body ?? {};
        if(typeof email !== 'string' || typeof password !== 'string' || !email || !password){
            throw new HttpError(400, 'Email y contraseña son obligatorios');
        }

        const r = await clientes.validarCredenciales({ email, password });
        if (!r.credencialesValidas || !r.rol) {
            throw new HttpError(401, 'Email o contraseña incorrectos');
        }
        const token = firmarToken({
            email: r.email ?? email,
            rol: r.rol as Rol,
            usuarioId: r.usuarioId ?? undefined,
            clienteId: r.clienteId ?? undefined,
        });
        res.json({ token });
    }),
);