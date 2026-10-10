import { status as Grpc } from '@grpc/grpc-js';

export class HttpError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

const GRPC_TO_HTTP: Partial<Record<number, number>> = {
  [Grpc.INVALID_ARGUMENT]: 400,
  [Grpc.UNAUTHENTICATED]: 401,
  [Grpc.PERMISSION_DENIED]: 403,
  [Grpc.NOT_FOUND]: 404,
  [Grpc.ALREADY_EXISTS]: 409,
  [Grpc.FAILED_PRECONDITION]: 409,
  [Grpc.UNAVAILABLE]: 503,
  [Grpc.DEADLINE_EXCEEDED]: 504,
};

export function toHttpError(err: unknown): { status: number; message: string } {
  if (err instanceof HttpError) return { status: err.status, message: err.message };

  const e = err as { code?: number; details?: string; message?: string };
  if (typeof e?.code === 'number' && e.code in Grpc) {
    const status = GRPC_TO_HTTP[e.code] ?? 500;
    const message =
      status === 503
        ? 'Servicio no disponible, intentá de nuevo en unos instantes'
        : e.details || e.message || 'Error';
    return { status, message };
  }
  return { status: 500, message: 'Error interno del servidor' };
}

// Mismo formato de error que el backend del Hito 1
export function errorBody(status: number, message: string) {
  return { timestamp: new Date().toISOString(), status, message };
}