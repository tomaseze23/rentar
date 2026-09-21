import type { Cliente, ClienteInput, ClienteUpdateInput } from '../types'
import { request } from './http'

export const clientesApi = {
  listar: (incluirInactivos = false) =>
    request<Cliente[]>(`/api/clientes?incluirInactivos=${incluirInactivos}`),
  crear: (input: ClienteInput) => request<Cliente>('/api/clientes', { method: 'POST', body: input }),
  actualizar: (id: number, input: ClienteUpdateInput) =>
    request<Cliente>(`/api/clientes/${id}`, { method: 'PUT', body: input }),
  baja: (id: number) => request<void>(`/api/clientes/${id}`, { method: 'DELETE' }),
}
