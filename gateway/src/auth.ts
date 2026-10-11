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

// Lee el header Authorization. Sin header devuelve null; con un token inválido o vencido lanza 401.
export function leerUsuario(header: string | undefined): UsuarioToken | null {
  if (!header) return null;
  if (!header.startsWith('Bearer ')) throw new HttpError(401, 'Token inválido o expirado');
  try {
    const p = jwt.verify(header.slice(7), config.jwtSecret) as jwt.JwtPayload;
    return { email: String(p.sub), rol: p.rol, usuarioId: p.usuarioId, clienteId: p.clienteId };
  } catch {
    throw new HttpError(401, 'Token inválido o expirado');
  }
}

// Middleware 1: ¿viene un token válido? Si sí, deja el usuario en req.usuario.
export function autenticar(req: Request, _res: Response, next: NextFunction) {
  try {
    const usuario = leerUsuario(req.header('Authorization'));
    if (!usuario) return next(new HttpError(401, 'Falta el token de autenticación'));
    req.usuario = usuario;
    next();
  } catch (e) {
    next(e);
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