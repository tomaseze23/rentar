import { useEffect, useState } from 'react'
import { reservasApi } from '../../api/reservas'
import { Alert, Badge, errorMessage } from '../../components/ui'
import { formatDateTime, formatMoney } from '../../format'
import type { HistorialAlquiler } from '../../types'

export function HistorialPage() {
  const [items, setItems] = useState<HistorialAlquiler[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    reservasApi
      .historial()
      .then(setItems)
      .catch((e) => setError(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [])

  return (
    <section>
      <div className="page-head">
        <h1>Historial de alquileres</h1>
      </div>
      <p className="muted">Alquileres finalizados y reservas canceladas.</p>

      {error && <Alert>{error}</Alert>}

      {loading ? (
        <p className="muted">Cargando…</p>
      ) : items.length === 0 ? (
        <p className="muted">Todavía no tenés alquileres finalizados ni reservas canceladas.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Vehículo</th>
                <th>Patente</th>
                <th>Inicio</th>
                <th>Fin</th>
                <th className="num">Días</th>
                <th className="num">Importe total</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {items.map((h) => (
                <tr key={h.id}>
                  <td>{h.vehiculo}</td>
                  <td className="mono">{h.patente}</td>
                  <td>{formatDateTime(h.fechaInicio)}</td>
                  <td>{formatDateTime(h.fechaFin)}</td>
                  <td className="num">{h.cantidadDias}</td>
                  <td className="num">{formatMoney(h.importeTotal)}</td>
                  <td>
                    <Badge tone={h.estado === 'FINALIZADA' ? 'blue' : 'gray'}>{h.estado}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
