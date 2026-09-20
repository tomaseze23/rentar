import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api/auth'
import { setUnauthorizedHandler, tokenStore } from '../api/http'
import { isExpired, parseToken, type Session } from './jwt'

interface AuthContextValue {
  session: Session | null
  login: (email: string, password: string) => Promise<Session>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function loadSession(): Session | null {
  const token = tokenStore.get()
  if (!token) return null
  const session = parseToken(token)
  if (!session || isExpired(session)) {
    tokenStore.clear()
    return null
  }
  return session
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(loadSession)

  const logout = useCallback(() => {
    tokenStore.clear()
    setSession(null)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const { token } = await authApi.login(email, password)
    const parsed = parseToken(token)
    if (!parsed) throw new Error('El servidor devolvió un token inválido.')
    tokenStore.set(token)
    setSession(parsed)
    return parsed
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
  }, [logout])

  useEffect(() => {
    if (!session) return
    const msLeft = Math.min(session.exp * 1000 - Date.now(), 2 ** 31 - 1)
    const timer = window.setTimeout(logout, Math.max(msLeft, 0))
    return () => window.clearTimeout(timer)
  }, [session, logout])

  const value = useMemo(() => ({ session, login, logout }), [session, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return context
}
