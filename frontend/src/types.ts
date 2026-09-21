export type Rol = 'ADMINISTRADOR' | 'CLIENTE'

export const TIPOS_VEHICULO = ['SEDAN', 'SUV', 'PICKUP', 'COUPE', 'HATCHBACK'] as const
export type TipoVehiculo = (typeof TIPOS_VEHICULO)[number]

export type EstadoVehiculo = 'DISPONIBLE' | 'RESERVADO' | 'EN_ALQUILER'

export const ESTADOS_RESERVA = ['PENDIENTE', 'CONFIRMADA', 'CANCELADA'] as const
export type EstadoReserva = (typeof ESTADOS_RESERVA)[number]

export interface Vehiculo {
  id: number
  patente: string
  marca: string
  modelo: string
  anio: string
  color: string | null
  tipoVehiculo: TipoVehiculo | null
  precioDiario: number | null
  estado: EstadoVehiculo | null
  activo: boolean | null
}

export interface VehiculoInput {
  patente: string
  marca: string
  modelo: string
  anio: string
  color: string | null
  tipoVehiculo: TipoVehiculo
  precioDiario: number
}

export interface Cliente {
  id: number
  usuarioId: number
  email: string
  documento: string
  nombre: string
  apellido: string
  telefono: string | null
  fechaNacimiento: string | null
  activo: boolean
}

export interface ClienteInput {
  email: string
  password: string
  documento: string
  nombre: string
  apellido: string
  telefono: string | null
  fechaNacimiento: string | null
}

export interface ClienteUpdateInput {
  nombre: string
  apellido: string
  telefono: string | null
  fechaNacimiento: string | null
}

export interface ReservaConsulta {
  id: string
  clienteId: string
  cliente: string
  vehiculoId: string
  vehiculo: string
  patente: string
  fechaInicio: string
  fechaFin: string
  precioDiario: string
  importeTotal: string
  estado: EstadoReserva
}

export interface ReservaFiltro {
  clienteId?: string
  vehiculoId?: string
  tipoVehiculo?: TipoVehiculo
  estado?: EstadoReserva
  fechaDesde?: string
  fechaHasta?: string
}

export interface VehiculoDisponible {
  id: string
  patente: string
  marca: string
  modelo: string
  anio: string
  color: string | null
  tipoVehiculo: TipoVehiculo | null
  precioDiario: number | null
}

export interface DisponibilidadFiltro {
  fechaInicio: string
  fechaFin: string
  tipoVehiculo?: TipoVehiculo
  marca?: string
  modelo?: string
  precioMin?: number
  precioMax?: number
}

export interface ReservaCreada {
  id: number
  clienteId: number
  vehiculoId: number
  fechaInicio: string
  fechaFin: string
  precioDiario: number
  importeTotal: number
  estado: EstadoReserva
}

export interface HistorialAlquiler {
  id: string
  vehiculo: string
  patente: string
  fechaInicio: string
  fechaFin: string
  cantidadDias: number
  importeTotal: string
  estado: 'FINALIZADA' | 'CANCELADA'
}
