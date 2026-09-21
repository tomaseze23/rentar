import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const ADMIN_LINKS = [
  { to: '/admin/vehiculos', label: 'Vehículos' },
  { to: '/admin/clientes', label: 'Clientes' },
  { to: '/admin/reservas', label: 'Reservas' },
]

const CLIENTE_LINKS = [
  { to: '/cliente/reservar', label: 'Buscar y reservar' },
  { to: '/cliente/reservas', label: 'Mis reservas' },
  { to: '/cliente/historial', label: 'Historial' },
]

export function Layout() {
  const { session, logout } = useAuth()
  const links = session?.rol === 'ADMINISTRADOR' ? ADMIN_LINKS : CLIENTE_LINKS

  return (
    <div className="app">
      <header className="topbar">
        <span className="brand">Rentar</span>
        <nav>
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} className={({ isActive }) => (isActive ? 'active' : '')}>
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="user">
          <span className="user-email">{session?.email}</span>
          <span className="role">{session?.rol === 'ADMINISTRADOR' ? 'Admin' : 'Cliente'}</span>
          <button type="button" className="btn" onClick={logout}>
            Salir
          </button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
