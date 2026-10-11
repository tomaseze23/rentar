import { NextFunction, Request, RequestHandler, Response } from 'express';

// Envuelve un handler async y manda cualquier error al manejador central de index.ts.
export const asyncHandler =
  (fn: (req: Request, res: Response) => Promise<unknown>): RequestHandler =>
  (req: Request, res: Response, next: NextFunction) => {
    fn(req, res).catch(next);
  };