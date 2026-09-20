import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { clientesApi } from '../api/clientes'
import { reservasApi } from '../api/reservas'
import { vehiculosApi } from '../api/vehiculos'
import { Alert, Badge, ConfirmDialog, errorMessage } from '../components/ui'
import { formatDateTime, formatMoney } from '../format'
import {
  ESTADOS_RESERVA,
  TIPOS_VEHICULO,
  type Cliente,
  type EstadoReserva,
  type ReservaConsulta,
  type ReservaFiltro,
  type TipoVehiculo,
  type Vehiculo,
} from '../types'

interface Props {
  modo: 'admin' | 'cliente'
}

interface FiltroForm {
  clienteId: string
  vehiculoId: string
  tipoVehiculo: '' | TipoVehiculo
  estado: '' | EstadoReserva
  fechaDesde: string
  fechaHasta: string
}

const FILTRO_VACIO: FiltroForm = {
  clienteId: '',
  vehiculoId: '',
  tipoVehiculo: '',
  estado: '',
  fechaDesde: '',
  fechaHasta: '',
}

const ESTADO_TONE = { PENDIENTE: 'amber', CONFIRMADA: 'green', CANCELADA: 'gray' } as const

function toFiltro(form: FiltroForm): ReservaFiltro {
  const filtro: ReservaFiltro = {}
  if (form.clienteId) filtro.clienteId = form.clienteId
  if (form.vehiculoId) filtro.vehiculoId = form.vehiculoId
  if (form.tipoVehiculo) filtro.tipoVehiculo = form.tipoVehiculo
  if (form.estado) filtro.estado = form.estado
  if (form.fechaDesde) filtro.fechaDesde = form.fechaDesde
  if (form.fechaHasta) filtro.fechaHasta = form.fechaHasta
  return filtro
}

export function ReservasPage({ modo }: Props) {
  const esAdmin = modo === 'admin'
  const [form, setForm] = useState<FiltroForm>(FILTRO_VACIO)
  const [reservas, setReservas] = useState<ReservaConsulta[]>([])
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [aCancelar, setACancelar] = useState<ReservaConsulta | null>(null)

  const buscar = useCallback(async (filtroForm: FiltroForm) => {
    setLoading(true)
    setError(null)
    try {
      setReservas(await reservasApi.consultar(toFiltro(filtroForm)))
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void buscar(FILTRO_VACIO)
    if (esAdmin) {
      void clientesApi.listar(true).then(setClientes, () => setClientes([]))
      void vehiculosApi.listar().then(setVehiculos, () => setVehiculos([]))
    }
  }, [buscar, esAdmin])

  const set = <K extends keyof FiltroForm>(key: K, value: FiltroForm[K]) => setForm((f) => ({ ...f, [key]: value }))

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    setNotice(null)
    void buscar(form)
  }

  function limpiar() {
    setForm(FILTRO_VACIO)
    setNotice(null)
    void buscar(FILTRO_VACIO)
  }

  async function confirmarCancelacion() {
    if (!aCancelar) return
    setError(null)
    setNotice(null)
    try {
      await reservasApi.cancelar(aCancelar.id)
      setNotice('Reserva cancelada. El vehículo vuelve a estar disponible para ese período.')
      await buscar(form)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setACancelar(null)
    }
  }

  const puedeCancelar = (r: ReservaConsulta) =>
    !esAdmin && r.estado === 'CONFIRMADA' && new Date(r.fechaInicio).getTime() > Date.now()

  return (
    <section>
      <div className="page-head">
        <h1>{esAdmin ? 'Reservas' : 'Mis reservas'}</h1>
      </div>

      <form className="card filters" onSubmit={onSubmit}>
        {esAdmin && (
          <label className="field">
            <span>Cliente</span>
            <select value={form.clienteId} onChange={(e) => set('clienteId', e.target.value)}>
              <option value="">Todos</option>
              {clientes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.apellido}, {c.nombre}
                </option>
              ))}
            </select>
          </label>
        )}
        {esAdmin && (
          <label className="field">
            <span>Vehículo</span>
            <select value={form.vehiculoId} onChange={(e) => set('vehiculoId', e.target.value)}>
              <option value="">Todos</option>
              {vehiculos.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.patente} · {v.marca} {v.modelo}
                </option>
              ))}
            </select>
          </label>
        )}
        <label className="field">
          <span>Tipo de vehículo</span>
          <select value={form.tipoVehiculo} onChange={(e) => set('tipoVehiculo', e.target.value as FiltroForm['tipoVehiculo'])}>
            <option value="">Todos</option>
            {TIPOS_VEHICULO.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Estado</span>
          <select value={form.estado} onChange={(e) => set('estado', e.target.value as FiltroForm['estado'])}>
            <option value="">Todos</option>
            {ESTADOS_RESERVA.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Desde</span>
          <input type="datetime-local" value={form.fechaDesde} onChange={(e) => set('fechaDesde', e.target.value)} />
        </label>
        <label className="field">
          <span>Hasta</span>
          <input type="datetime-local" value={form.fechaHasta} onChange={(e) => set('fechaHasta', e.target.value)} />
        </label>
        <div className="filter-actions">
          <button type="submit" className="btn primary">
            Buscar
          </button>
          <button type="button" className="btn" onClick={limpiar}>
            Limpiar
          </button>
        </div>
      </form>

      {notice && <Alert kind="success">{notice}</Alert>}
      {error && <Alert>{error}</Alert>}

      {loading ? (
        <p className="muted">Cargando…</p>
      ) : reservas.length === 0 ? (
        <p className="muted">
          No hay reservas para los filtros seleccionados.{' '}
          {!esAdmin && <Link to="/cliente/reservar">Buscá un vehículo y reservá</Link>}
        </p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                {esAdmin && <th>Cliente</th>}
                <th>Vehículo</th>
                <th>Patente</th>
                <th>Inicio</th>
                <th>Fin</th>
                <th className="num">Precio diario</th>
                <th className="num">Importe total</th>
                <th>Estado</th>
                {!esAdmin && <th />}
              </tr>
            </thead>
            <tbody>
              {reservas.map((r) => (
                <tr key={r.id}>
                  {esAdmin && <td>{r.cliente}</td>}
                  <td>{r.vehiculo}</td>
                  <td className="mono">{r.patente}</td>
                  <td>{formatDateTime(r.fechaInicio)}</td>
                  <td>{formatDateTime(r.fechaFin)}</td>
                  <td className="num">{formatMoney(r.precioDiario)}</td>
                  <td className="num">{formatMoney(r.importeTotal)}</td>
                  <td>
                    <Badge tone={ESTADO_TONE[r.estado]}>{r.estado}</Badge>
                  </td>
                  {!esAdmin && (
                    <td className="row-actions">
                      {puedeCancelar(r) && (
                        <button type="button" className="btn small danger" onClick={() => setACancelar(r)}>
                          Cancelar
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {aCancelar && (
        <ConfirmDialog
          title="Cancelar reserva"
          message={`¿Cancelar la reserva de ${aCancelar.vehiculo} (${aCancelar.patente}) del ${formatDateTime(aCancelar.fechaInicio)} al ${formatDateTime(aCancelar.fechaFin)}?`}
          confirmLabel="Cancelar reserva"
          onConfirm={confirmarCancelacion}
          onCancel={() => setACancelar(null)}
        />
      )}
    </section>
  )
}
