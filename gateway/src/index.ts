import express, { NextFunction, Request, Response } from 'express';
import { config } from './config';
import { errorBody, HttpError,toHttpError } from './errors';
import { vehiculosRouter } from './routes/vehiculos';
import { authRouter } from './routes/auth';
import { autenticar, requerirRol } from './auth';
import { clientesRouter } from './routes/clientes';
import { reservasRouter } from './routes/reservas';
import { graphqlHandler } from './graphql';
import cors from 'cors';

const app = express();

app.use(cors({ origin: config.corsOrigin }));

// 1. Middlewares generales (leen el body JSON de las requests)
app.use(express.json());

// 2. Rutas
app.get('/health', (_req, res) => {
  res.json({ status: 'ok' });
});

//REST
app.use('/api/auth', authRouter);
app.use('/api/vehiculos', autenticar, requerirRol('ADMINISTRADOR'), vehiculosRouter);
app.use('/api/clientes', autenticar, requerirRol('ADMINISTRADOR'), clientesRouter);
app.use('/api/reservas', autenticar, requerirRol('CLIENTE'), reservasRouter);

//GraphQL
app.all('/graphql', graphqlHandler);

// Cualquier ruta que no exista: 404 con el mismo formato de error
app.use((_req, _res, next) => next(new HttpError(404, 'Ruta no encontrada')));

// 3. Manejo de errores: SIEMPRE al final, después de todas las rutas
app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
  const { status, message } = toHttpError(err);
  if (status >= 500) console.error(err); // loguea los errores inesperados
  res.status(status).json(errorBody(status, message));
});

app.listen(config.port, () => {
  console.log(`Gateway escuchando en http://localhost:${config.port}`);
});