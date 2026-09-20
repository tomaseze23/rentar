import { isExpired, parseToken } from '../auth/jwt'

const TOKEN_KEY = 'rentar.token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

let onUnauthorized: () => void = () => {}
export const setUnauthorizedHandler = (handler: () => void) => {
  onUnauthorized = handler
}

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

const STATUS_MESSAGES: Record<number, string> = {
  0: 'No se pudo conectar con el servidor. Verificá que el backend esté corriendo.',
  400: 'Los datos enviados no son válidos.',
  401: 'Tu sesión expiró. Volvé a iniciar sesión.',
  403: 'No tenés permisos para realizar esta acción.',
  404: 'No se encontró el recurso solicitado.',
  409: 'La operación entra en conflicto con el estado actual.',
  422: 'La operación no se puede realizar.',
  500: 'Ocurrió un error inesperado en el servidor.',
}

// El backend responde con formatos distintos: { errores: {campo: msg} }, { message } o el error por defecto de Spring.
function toApiError(status: number, body: unknown): ApiError {
  const data = (body ?? {}) as { errores?: Record<string, string>; message?: string }
  if (data.errores && Object.keys(data.errores).length > 0) {
    return new ApiError(status, Object.values(data.errores).join(' · '))
  }
  if (data.message) return new ApiError(status, data.message)
  return new ApiError(status, STATUS_MESSAGES[status] ?? `Error ${status}`)
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

interface RequestOptions {
  method?: string
  body?: unknown
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {}

  const token = tokenStore.get()
  if (token) {
    const session = parseToken(token)
    if (!session || isExpired(session)) {
      onUnauthorized()
      throw new ApiError(401, STATUS_MESSAGES[401])
    }
    headers.Authorization = `Bearer ${token}`
  }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'

  let response: Response
  try {
    response = await fetch(path, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    })
  } catch {
    throw new ApiError(0, STATUS_MESSAGES[0])
  }

  const text = response.status === 204 ? '' : await response.text()
  const data = text ? safeParse(text) : undefined

  if (!response.ok) throw toApiError(response.status, data)
  return data as T
}

interface GraphQLResponse<T> {
  data?: T
  errors?: { message: string; extensions?: { classification?: string } }[]
}

export async function gql<T>(query: string, variables?: Record<string, unknown>): Promise<T> {
  const result = await request<GraphQLResponse<T>>('/graphql', {
    method: 'POST',
    body: { query, variables },
  })

  if (result.errors?.length) {
    const unauthorized = result.errors.some((e) => e.extensions?.classification === 'UNAUTHORIZED')
    if (unauthorized) onUnauthorized()
    const forbidden = result.errors.some((e) => e.extensions?.classification === 'FORBIDDEN')
    const status = unauthorized ? 401 : forbidden ? 403 : 400
    throw new ApiError(status, result.errors.map((e) => e.message).join(' · '))
  }
  return result.data as T
}
