import { useCallback, useEffect, useState } from 'react'
import { clientesApi } from '../../api/clientes'
import { vehiculosExternosApi } from '../../api/vehiculosExternos'
import { Alert, Badge, errorMessage } from '../../components/ui'
import { formatMoney } from '../../format'
import type { Cliente, EstadoSolicitud, ResultadoValidacion, SolicitudIncorporacion } from '../../types'

const ESTADO_TONE: Record<EstadoSolicitud, 'amber' | 'red' | 'green'> = {
  PENDIENTE: 'amber',
  RECHAZADA: 'red',
  INCORPORADA: 'green',
}

const VALIDACION_TONE: Record<ResultadoValidacion, 'amber' | 'red' | 'green'> = {
  PENDIENTE: 'amber',
  RECHAZADO: 'red',
  VALIDADO: 'green',
}

export function SolicitudesPage() {
  const [items, setItems] = useState<SolicitudIncorporacion[]>([])
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [clienteId, setClienteId] = useState<number | undefined>(undefined)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Los clientes se piden una sola vez, para mostrar nombre y apellido en lugar del id.
  useEffect(() => {
    clientesApi
      .listar(true)
      .then(setClientes)
      .catch((e) => setError(errorMessage(e)))
  }, [])

  const cargar = useCallback(async () => {
    try {
      setItems(await vehiculosExternosApi.listar(clienteId))
      setError(null)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [clienteId])

  useEffect(() => {
    setLoading(true)
    void cargar()
  }, [cargar])

  // Mientras haya solicitudes pendientes se refresca solo, igual que en la pantalla del cliente.
  const hayPendientes = items.some((s) => s.estado === 'PENDIENTE')
  useEffect(() => {
    if (!hayPendientes) return
    const timer = setInterval(() => void cargar(), 3000)
    return () => clearInterval(timer)
  }, [hayPendientes, cargar])

  const nombreDe = (id: number) => {
    const c = clientes.find((x) => x.id === id)
    return c ? `${c.nombre} ${c.apellido}` : `Cliente #${id}`
  }

  return (
    <section>
      <div className="page-head">
        <h1>Solicitudes de incorporación</h1>
        <div className="actions">
          <select
            value={clienteId ?? ''}
            onChange={(e) => setClienteId(e.target.value === '' ? undefined : Number(e.target.value))}
            aria-label="Filtrar por cliente"
          >
            <option value="">Todos los clientes</option>
            {clientes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre} {c.apellido}
              </option>
            ))}
          </select>
        </div>
      </div>
      <p className="muted">Vehículos que los clientes propusieron incorporar a la flota y el resultado de sus validaciones.</p>

      {error && <Alert>{error}</Alert>}

      {loading ? (
        <p className="muted">Cargando…</p>
      ) : items.length === 0 ? (
        <p className="muted">No hay solicitudes para mostrar.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Cliente</th>
                <th>Patente</th>
                <th>Vehículo</th>
                <th className="num">Precio diario</th>
                <th>Patente</th>
                <th>Titularidad</th>
                <th>Estado</th>
                <th className="num">Vehículo #</th>
              </tr>
            </thead>
            <tbody>
              {items.map((s) => (
                <tr key={s.id}>
                  <td>{nombreDe(s.clienteId)}</td>
                  <td className="mono">{s.patente}</td>
                  <td>
                    {s.marca} {s.modelo} ({s.anio})
                  </td>
                  <td className="num">{formatMoney(s.precioDiario)}</td>
                  <td>
                    <Badge tone={VALIDACION_TONE[s.validacionPatente]}>{s.validacionPatente}</Badge>
                  </td>
                  <td>
                    <Badge tone={VALIDACION_TONE[s.validacionTitularidad]}>{s.validacionTitularidad}</Badge>
                  </td>
                  <td>
                    <Badge tone={ESTADO_TONE[s.estado]}>{s.estado}</Badge>
                  </td>
                  <td className="num">{s.vehiculoId ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}