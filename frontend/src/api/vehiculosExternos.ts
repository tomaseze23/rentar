import type { SolicitudIncorporacion, VehiculoInput } from '../types'
import { request } from './http'

export const vehiculosExternosApi = {
  listar: () => request<SolicitudIncorporacion[]>('/api/vehiculos-externos'),
  proponer: (input: VehiculoInput) => 
    request<SolicitudIncorporacion>('/api/vehiculos-externos', { method: 'POST', body: input }),
}
