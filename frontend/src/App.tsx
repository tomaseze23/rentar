import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import { RequireRole, homeFor } from './auth/RequireRole'
import { Layout } from './components/Layout'
import { Login } from './pages/Login'
import { ReservasPage } from './pages/ReservasPage'
import { ClientesPage } from './pages/admin/ClientesPage'
import { VehiculosPage } from './pages/admin/VehiculosPage'
import { DisponibilidadPage } from './pages/cliente/DisponibilidadPage'
import { HistorialPage } from './pages/cliente/HistorialPage'

function Home() {
  const { session } = useAuth()
  return <Navigate to={session ? homeFor(session.rol) : '/login'} replace />
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<RequireRole roles={['ADMINISTRADOR']} />}>
        <Route path="/admin" element={<Layout />}>
          <Route index element={<Navigate to="vehiculos" replace />} />
          <Route path="vehiculos" element={<VehiculosPage />} />
          <Route path="clientes" element={<ClientesPage />} />
          <Route path="reservas" element={<ReservasPage modo="admin" />} />
        </Route>
      </Route>

      <Route element={<RequireRole roles={['CLIENTE']} />}>
        <Route path="/cliente" element={<Layout />}>
          <Route index element={<Navigate to="reservas" replace />} />
          <Route path="reservar" element={<DisponibilidadPage />} />
          <Route path="reservas" element={<ReservasPage modo="cliente" />} />
          <Route path="historial" element={<HistorialPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Home />} />
    </Routes>
  )
}
