import { NextFunction, Request, RequestHandler, Response } from 'express';
import { HttpError } from './errors';

// Envuelve un handler async y manda cualquier error al manejador central de index.ts.
export const asyncHandler =
  (fn: (req: Request, res: Response) => Promise<unknown>): RequestHandler =>
  (req: Request, res: Response, next: NextFunction) => {
    fn(req, res).catch(next);
  };

export function idNumerico(valor: unknown): string {
  const id = String(valor);
  if (!/^\d+$/.test(id)) throw new HttpError(400, 'El id debe ser numérico');
  return id;
}