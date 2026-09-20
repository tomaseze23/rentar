import type { Vehiculo, VehiculoInput } from '../types'
import { request } from './http'

export const vehiculosApi = {
  listar: (activo?: boolean) =>
    request<Vehiculo[]>(`/api/vehiculos${activo === undefined ? '' : `?activo=${activo}`}`),
  crear: (input: VehiculoInput) => request<Vehiculo>('/api/vehiculos', { method: 'POST', body: input }),
  modificar: (id: number, input: VehiculoInput) =>
    request<Vehiculo>(`/api/vehiculos/${id}`, { method: 'PUT', body: input }),
  baja: (id: number) => request<void>(`/api/vehiculos/${id}`, { method: 'DELETE' }),
}
