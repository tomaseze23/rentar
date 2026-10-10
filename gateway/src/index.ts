import express, { NextFunction, Request, Response } from 'express';
import { config } from './config';
import { errorBody, toHttpError } from './errors';

const app = express();

app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
  const { status, message } = toHttpError(err);
  res.status(status).json(errorBody(status, message));
});

app.get('/health', (_req, res) => {
  res.json({ status: 'ok' });
});

app.listen(config.port, () => {
  console.log(`Gateway escuchando en http://localhost:${config.port}`);
});