import type {
  DisponibilidadFiltro,
  HistorialAlquiler,
  ReservaConsulta,
  ReservaCreada,
  ReservaFiltro,
  VehiculoDisponible,
} from '../types'
import { toDateTimeInput } from '../format'
import { gql, request } from './http'

const RESERVAS_QUERY = `
  query Reservas($filtro: ReservaFiltro) {
    reservas(filtro: $filtro) {
      id clienteId cliente vehiculoId vehiculo patente
      fechaInicio fechaFin precioDiario importeTotal estado
    }
  }
`

const HISTORIAL_QUERY = `
  query Historial {
    historialAlquileres {
      id vehiculo patente fechaInicio fechaFin cantidadDias importeTotal estado
    }
  }
`

const DISPONIBILIDAD_QUERY = `
  query Disponibilidad($filtro: DisponibilidadFiltro!) {
    vehiculosDisponibles(filtro: $filtro) {
      id patente marca modelo anio color tipoVehiculo precioDiario
    }
  }
`

export const reservasApi = {
  async consultar(filtro: ReservaFiltro): Promise<ReservaConsulta[]> {
    const data = await gql<{ reservas: ReservaConsulta[] }>(RESERVAS_QUERY, { filtro })
    return data.reservas
  },

  // Reservas confirmadas que todavía no terminaron (en curso o futuras) de un vehículo o de un cliente.
  async contarVigentes(filtro: { vehiculoId?: string; clienteId?: string }): Promise<number> {
    const reservas = await reservasApi.consultar({
      ...filtro,
      estado: 'CONFIRMADA',
      fechaDesde: toDateTimeInput(new Date()),
    })
    return reservas.length
  },

  async historial(): Promise<HistorialAlquiler[]> {
    const data = await gql<{ historialAlquileres: HistorialAlquiler[] }>(HISTORIAL_QUERY)
    return data.historialAlquileres
  },

  async disponibilidad(filtro: DisponibilidadFiltro): Promise<VehiculoDisponible[]> {
    const data = await gql<{ vehiculosDisponibles: VehiculoDisponible[] }>(DISPONIBILIDAD_QUERY, { filtro })
    return data.vehiculosDisponibles
  },

  // El cliente lo determina el backend a partir del token.
  crear: (input: { vehiculoId: string; fechaInicio: string; fechaFin: string }) =>
    request<ReservaCreada>('/api/reservas', {
      method: 'POST',
      body: { ...input, vehiculoId: Number(input.vehiculoId) },
    }),

  cancelar: (id: string) => request<void>(`/api/reservas/${id}`, { method: 'DELETE' }),
}
