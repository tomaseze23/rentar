import { HttpError } from './errors';
import type { TipoVehiculo } from './dominio';

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

export interface DatosCliente {
  email?: string;
  password?: string;
  documento?: string;
  nombre: string;
  apellido: string;
  telefono?: string;
  fechaNacimiento?: string;
}

function fechaValida(s: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false;
  const d = new Date(`${s}T00:00:00Z`);
  return !isNaN(d.getTime()) && d.toISOString().startsWith(s);
}

// Alta (esAlta = true): email, password y documento obligatorios. Modificación: solo nombre y apellido.
export function validarCliente(body: any, esAlta: boolean): DatosCliente {
  const errores: string[] = [];
  const texto = (v: unknown) => (typeof v === 'string' ? v.trim() : '');

  const email = texto(body?.email);
  const password = typeof body?.password === 'string' ? body.password : '';
  const documento = texto(body?.documento);
  if (esAlta) {
    if (!email) errores.push('El email es obligatorio');
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errores.push('El email no es válido');
    if (!password) errores.push('La contraseña es obligatoria');
    if (!documento) errores.push('El documento es obligatorio');
  }

  const nombre = texto(body?.nombre);
  if (!nombre) errores.push('El nombre es obligatorio');
  const apellido = texto(body?.apellido);
  if (!apellido) errores.push('El apellido es obligatorio');

  const fecha = texto(body?.fechaNacimiento);
  if (fecha && !fechaValida(fecha)) errores.push('La fecha de nacimiento debe tener formato yyyy-MM-dd');

  if (errores.length > 0) throw new HttpError(400, errores.join(', '));

  return {
    email: email || undefined,
    password: password || undefined,
    documento: documento || undefined,
    nombre,
    apellido,
    telefono: texto(body?.telefono) || undefined,
    fechaNacimiento: fecha || undefined,
  };
}

export interface DatosReserva {
  vehiculoId: string;
  fechaInicio: string;
  fechaFin: string;
}

// Acepta yyyy-MM-ddTHH:mm o yyyy-MM-ddTHH:mm:ss (hora local, sin zona). Devuelve siempre con segundos.
const FECHA_HORA = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/;
const conSegundos = (s: string) => (s.length === 16 ? `${s}:00` : s);

export function validarReserva(body: any): DatosReserva {
  const errores: string[] = [];

  const idRaw = body?.vehiculoId;
  const vehiculoId = idRaw === undefined || idRaw === null ? '' : String(idRaw).trim();
  if (!vehiculoId) errores.push('El ID del vehículo es obligatorio');
  else if (!/^\d+$/.test(vehiculoId)) errores.push('El ID del vehículo debe ser numérico');

  const leer = (valor: unknown, nombre: string): string | null => {
    if (typeof valor !== 'string' || !valor.trim()) {
      errores.push(`La fecha de ${nombre} es obligatoria`);
      return null;
    }
    const v = valor.trim();
    if (!FECHA_HORA.test(v) || isNaN(new Date(v).getTime())) {
      errores.push(`La fecha de ${nombre} debe tener formato yyyy-MM-ddTHH:mm`);
      return null;
    }
    return conSegundos(v);
  };
  const inicio = leer(body?.fechaInicio, 'inicio');
  const fin = leer(body?.fechaFin, 'fin');

  if (inicio && fin) {
    if (new Date(fin).getTime() <= new Date(inicio).getTime()) {
      errores.push('La fecha de fin debe ser posterior a la fecha de inicio');
    }
    if (new Date(inicio).getTime() < Date.now()) {
      errores.push('La fecha de inicio debe ser futura');
    }
  }

  if (errores.length > 0) throw new HttpError(400, errores.join(', '));
  return { vehiculoId, fechaInicio: inicio!, fechaFin: fin! };
}

// Fecha con hora, sin zona: yyyy-MM-ddTHH:mm o yyyy-MM-ddTHH:mm:ss. Devuelve siempre con segundos.
const FECHA_HORA_GQL = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/;
export function leerFechaHora(valor: unknown, nombre: string): string {
  if (typeof valor !== 'string' || !valor.trim()) throw new HttpError(400, `La fecha de ${nombre} es obligatoria`);
  const v = valor.trim();
  if (!FECHA_HORA_GQL.test(v) || isNaN(new Date(v).getTime())) {
    throw new HttpError(400, `La fecha de ${nombre} debe tener formato yyyy-MM-ddTHH:mm`);
  }
  return v.length === 16 ? `${v}:00` : v;
}