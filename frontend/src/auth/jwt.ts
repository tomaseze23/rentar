import type { Rol } from '../types'

export interface Session {
  token: string
  email: string
  rol: Rol
  exp: number
}

export function parseToken(token: string): Session | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const binary = atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '='))
    const json = new TextDecoder().decode(Uint8Array.from(binary, (c) => c.charCodeAt(0)))
    const claims = JSON.parse(json) as { sub?: string; rol?: string; exp?: number }
    if (!claims.sub || !claims.exp) return null
    if (claims.rol !== 'ADMINISTRADOR' && claims.rol !== 'CLIENTE') return null
    return { token, email: claims.sub, rol: claims.rol, exp: claims.exp }
  } catch {
    return null
  }
}

export const isExpired = (session: Pick<Session, 'exp'>) => session.exp * 1000 <= Date.now()
