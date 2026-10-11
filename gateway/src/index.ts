import express, { NextFunction, Request, Response } from 'express';
import { config } from './config';
import { errorBody, toHttpError } from './errors';
import { vehiculosRouter } from './routes/vehiculos';

const app = express();

// 1. Middlewares generales (leen el body JSON de las requests)
app.use(express.json());

// 2. Rutas
app.get('/health', (_req, res) => {
  res.json({ status: 'ok' });
});

app.use('/api/vehiculos', vehiculosRouter);

// 3. Manejo de errores: SIEMPRE al final, después de todas las rutas
app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
  const { status, message } = toHttpError(err);
  if (status >= 500) console.error(err); // loguea los errores inesperados
  res.status(status).json(errorBody(status, message));
});

app.listen(config.port, () => {
  console.log(`Gateway escuchando en http://localhost:${config.port}`);
});