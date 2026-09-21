import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { Rol } from '../types'
import { useAuth } from './AuthContext'

export const homeFor = (rol: Rol) => (rol === 'ADMINISTRADOR' ? '/admin/vehiculos' : '/cliente/reservas')

export function RequireRole({ roles }: { roles: Rol[] }) {
  const { session } = useAuth()
  const location = useLocation()

  if (!session) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (!roles.includes(session.rol)) return <Navigate to={homeFor(session.rol)} replace />
  return <Outlet />
}
