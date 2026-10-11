import { NextFunction, Request, Response } from 'express';
import jwt from 'jsonwebtoken';
import { config } from './config';
import { HttpError } from './errors';

export type Rol = 'ADMINISTRADOR' | 'CLIENTE';

//datos que viajan dentro del token.
export interface UsuarioToken {
    email: string;
    rol: Rol;
    usuarioId?: string;
    clienteId?: string; //admin no tiene.
}

//agrega 'req.usuario' al tipo request de express
declare global {
    namespace Express {
        interface Request {
            usuario?: UsuarioToken;
        }
    }
}

export function firmarToken(u: UsuarioToken): string {
    return jwt.sign(
        { rol: u.rol, usuarioId: u.usuarioId, clienteId: u.clienteId },
        config.jwtSecret,
        { subject: u.email, expiresIn: '24h' },
    );
}

// Middleware 1: ¿viene un token válido? Si sí, deja el usuario en req.usuario.
export function autenticar(req: Request, _res: Response, next: NextFunction) {
  const header = req.header('Authorization');
  if (!header?.startsWith('Bearer ')) {
    return next(new HttpError(401, 'Falta el token de autenticación'));
  }
  try {
    const p = jwt.verify(header.slice(7), config.jwtSecret) as jwt.JwtPayload;
    req.usuario = { email: String(p.sub), rol: p.rol, usuarioId: p.usuarioId, clienteId: p.clienteId };
    next();
  } catch {
    next(new HttpError(401, 'Token inválido o expirado'));
  }
}

// Middleware 2: ¿el rol del usuario alcanza? Se usa después de `autenticar`.
export const requerirRol =
  (...roles: Rol[]) =>
  (req: Request, _res: Response, next: NextFunction) => {
    if (!req.usuario || !roles.includes(req.usuario.rol)) {
      return next(new HttpError(403, 'No tenés permisos para realizar esta acción'));
    }
    next();
  };