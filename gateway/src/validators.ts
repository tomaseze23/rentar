import { HttpError } from './errors';
import type { TipoVehiculo } from './generated/rentar/comun/v1/TipoVehiculo';

export const TIPOS_VEHICULO = ['SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK'];

export interface DatosVehiculo {
  patente?: string;
  marca: string;
  modelo: string;
  anio: string;
  color?: string;
  tipoVehiculo: TipoVehiculo;
  precioDiario: number;
}

// Valida el body de alta (patente obligatoria) o de modificación (patente opcional).
export function validarVehiculo(body: any, opts: { patenteObligatoria: boolean }): DatosVehiculo {
  const errores: string[] = [];
  const texto = (v: unknown) => (typeof v === 'string' ? v.trim() : '');

  const patente = texto(body?.patente);
  if (opts.patenteObligatoria && !patente) errores.push('La patente es obligatoria');
  if (patente && patente.length > 10) errores.push('La patente no puede superar los 10 caracteres');
  if (patente && !/^[A-Za-z0-9 ]+$/.test(patente)) errores.push('La patente solo puede contener letras y números');

  const marca = texto(body?.marca);
  if (!marca) errores.push('La marca es obligatoria');

  const modelo = texto(body?.modelo);
  if (!modelo) errores.push('El modelo es obligatorio');

  const anio = texto(body?.anio);
  if (!anio) errores.push('El año es obligatorio');
  else if (!/^\d{4}$/.test(anio)) errores.push('El año debe tener 4 dígitos');

  const tipo = texto(body?.tipoVehiculo);
  if (!tipo) errores.push('El tipo de vehículo es obligatorio');
  else if (!TIPOS_VEHICULO.includes(tipo)) errores.push(`Tipo de vehículo inválido: ${tipo}`);

  const precio = body?.precioDiario;
  if (precio === undefined || precio === null) errores.push('El precio diario es obligatorio');
  else if (typeof precio !== 'number' || precio <= 0) errores.push('El precio diario debe ser mayor a 0');

  if (errores.length > 0) throw new HttpError(400, errores.join(', '));

  return {
    patente: patente || undefined,
    marca,
    modelo,
    anio,
    color: texto(body?.color) || undefined,
    tipoVehiculo: tipo as TipoVehiculo,
    precioDiario: precio,
  };
}