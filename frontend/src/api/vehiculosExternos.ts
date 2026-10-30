import type { SolicitudIncorporacion, VehiculoInput } from '../types'
import { request } from './http'

export const vehiculosExternosApi = {
  listar: (clienteId?: number) => 
    request<SolicitudIncorporacion[]>(`/api/vehiculos-externos${clienteId === undefined ? '' : `?clienteId=${clienteId}`}`),
  proponer: (input: VehiculoInput) => 
    request<SolicitudIncorporacion>('/api/vehiculos-externos', { method: 'POST', body: input }),
}
