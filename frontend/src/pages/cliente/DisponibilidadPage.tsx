import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { reservasApi } from '../../api/reservas'
import { Alert, Field, Modal, errorMessage } from '../../components/ui'
import { formatDateTime, formatMoney, toDateTimeInput } from '../../format'
import {
  TIPOS_VEHICULO,
  type DisponibilidadFiltro,
  type ReservaCreada,
  type TipoVehiculo,
  type VehiculoDisponible,
} from '../../types'

interface FormState {
  fechaInicio: string
  fechaFin: string
  tipoVehiculo: '' | TipoVehiculo
  marca: string
  modelo: string
  precioMin: string
  precioMax: string
}

function formInicial(): FormState {
  const inicio = new Date()
  inicio.setDate(inicio.getDate() + 1)
  inicio.setHours(10, 0, 0, 0)
  const fin = new Date(inicio)
  fin.setDate(fin.getDate() + 2)
  return {
    fechaInicio: toDateTimeInput(inicio),
    fechaFin: toDateTimeInput(fin),
    tipoVehiculo: '',
    marca: '',
    modelo: '',
    precioMin: '',
    precioMax: '',
  }
}

interface Busqueda {
  fechaInicio: string
  fechaFin: string
  resultados: VehiculoDisponible[]
}

function validar(form: FormState): string | null {
  if (!form.fechaInicio || !form.fechaFin) return 'Indicá la fecha y hora de inicio y de finalización.'
  if (new Date(form.fechaFin) <= new Date(form.fechaInicio)) return 'La finalización debe ser posterior al inicio.'
  if (new Date(form.fechaInicio) <= new Date()) return 'La fecha de inicio debe ser futura.'
  const min = form.precioMin === '' ? null : Number(form.precioMin)
  const max = form.precioMax === '' ? null : Number(form.precioMax)
  if ((min !== null && Number.isNaN(min)) || (max !== null && Number.isNaN(max))) return 'El rango de precio no es válido.'
  if (min !== null && max !== null && min > max) return 'El precio mínimo no puede superar al máximo.'
  return null
}

function toFiltro(form: FormState): DisponibilidadFiltro {
  const filtro: DisponibilidadFiltro = { fechaInicio: form.fechaInicio, fechaFin: form.fechaFin }
  if (form.tipoVehiculo) filtro.tipoVehiculo = form.tipoVehiculo
  if (form.marca.trim()) filtro.marca = form.marca.trim()
  if (form.modelo.trim()) filtro.modelo = form.modelo.trim()
  if (form.precioMin !== '') filtro.precioMin = Number(form.precioMin)
  if (form.precioMax !== '') filtro.precioMax = Number(form.precioMax)
  return filtro
}

export function DisponibilidadPage() {
  const [form, setForm] = useState<FormState>(formInicial)
  const [busqueda, setBusqueda] = useState<Busqueda | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [creada, setCreada] = useState<{ reserva: ReservaCreada; vehiculo: VehiculoDisponible } | null>(null)
  const [aReservar, setAReservar] = useState<VehiculoDisponible | null>(null)

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) => setForm((f) => ({ ...f, [key]: value }))

  async function buscar(estado: FormState) {
    setLoading(true)
    setError(null)
    try {
      const resultados = await reservasApi.disponibilidad(toFiltro(estado))
      setBusqueda({ fechaInicio: estado.fechaInicio, fechaFin: estado.fechaFin, resultados })
    } catch (e) {
      setBusqueda(null)
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    const problema = validar(form)
    if (problema) {
      setError(problema)
      return
    }
    setCreada(null)
    void buscar(form)
  }

  async function onReservada(reserva: ReservaCreada, vehiculo: VehiculoDisponible) {
    setAReservar(null)
    setCreada({ reserva, vehiculo })
    await buscar(form)
  }

  return (
    <section>
      <div className="page-head">
        <h1>Buscar y reservar</h1>
      </div>

      <form className="card filters" onSubmit={onSubmit}>
        <Field label="Inicio">
          <input type="datetime-local" value={form.fechaInicio} onChange={(e) => set('fechaInicio', e.target.value)} required />
        </Field>
        <Field label="Finalización">
          <input type="datetime-local" value={form.fechaFin} onChange={(e) => set('fechaFin', e.target.value)} required />
        </Field>
        <Field label="Tipo de vehículo">
          <select value={form.tipoVehiculo} onChange={(e) => set('tipoVehiculo', e.target.value as FormState['tipoVehiculo'])}>
            <option value="">Todos</option>
            {TIPOS_VEHICULO.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Marca">
          <input value={form.marca} onChange={(e) => set('marca', e.target.value)} />
        </Field>
        <Field label="Modelo">
          <input value={form.modelo} onChange={(e) => set('modelo', e.target.value)} />
        </Field>
        <Field label="Precio diario mínimo">
          <input value={form.precioMin} onChange={(e) => set('precioMin', e.target.value)} inputMode="decimal" />
        </Field>
        <Field label="Precio diario máximo">
          <input value={form.precioMax} onChange={(e) => set('precioMax', e.target.value)} inputMode="decimal" />
        </Field>
        <div className="filter-actions">
          <button type="submit" className="btn primary" disabled={loading}>
            {loading ? 'Buscando…' : 'Buscar'}
          </button>
        </div>
      </form>

      {creada && (
        <Alert kind="success">
          Reserva confirmada: {creada.vehiculo.marca} {creada.vehiculo.modelo} ({creada.vehiculo.patente}) del{' '}
          {formatDateTime(creada.reserva.fechaInicio)} al {formatDateTime(creada.reserva.fechaFin)}. Importe total:{' '}
          <strong>{formatMoney(creada.reserva.importeTotal)}</strong>. <Link to="/cliente/reservas">Ver mis reservas</Link>
        </Alert>
      )}
      {error && <Alert>{error}</Alert>}

      {busqueda &&
        (busqueda.resultados.length === 0 ? (
          <p className="muted">No hay vehículos disponibles para el período y los filtros elegidos.</p>
        ) : (
          <>
            <p className="muted">
              {busqueda.resultados.length} vehículo(s) disponible(s) del {formatDateTime(busqueda.fechaInicio)} al{' '}
              {formatDateTime(busqueda.fechaFin)}.
            </p>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Patente</th>
                    <th>Vehículo</th>
                    <th>Año</th>
                    <th>Color</th>
                    <th>Tipo</th>
                    <th className="num">Precio diario</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {busqueda.resultados.map((v) => (
                    <tr key={v.id}>
                      <td className="mono">{v.patente}</td>
                      <td>
                        {v.marca} {v.modelo}
                      </td>
                      <td>{v.anio}</td>
                      <td>{v.color ?? '—'}</td>
                      <td>{v.tipoVehiculo ?? '—'}</td>
                      <td className="num">{formatMoney(v.precioDiario)}</td>
                      <td className="row-actions">
                        <button type="button" className="btn small primary" onClick={() => setAReservar(v)}>
                          Reservar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        ))}

      {aReservar && busqueda && (
        <ConfirmarReserva
          vehiculo={aReservar}
          fechaInicio={busqueda.fechaInicio}
          fechaFin={busqueda.fechaFin}
          onClose={() => setAReservar(null)}
          onReservada={onReservada}
        />
      )}
    </section>
  )
}

function ConfirmarReserva({
  vehiculo,
  fechaInicio,
  fechaFin,
  onClose,
  onReservada,
}: {
  vehiculo: VehiculoDisponible
  fechaInicio: string
  fechaFin: string
  onClose: () => void
  onReservada: (reserva: ReservaCreada, vehiculo: VehiculoDisponible) => Promise<void>
}) {
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function confirmar() {
    setSaving(true)
    setError(null)
    try {
      const reserva = await reservasApi.crear({ vehiculoId: vehiculo.id, fechaInicio, fechaFin })
      await onReservada(reserva, vehiculo)
    } catch (e) {
      setError(errorMessage(e))
      setSaving(false)
    }
  }

  return (
    <Modal title="Confirmar reserva" onClose={onClose}>
      <div className="form-grid">
        {error && <Alert>{error}</Alert>}
        <Field label="Vehículo">
          <strong>
            {vehiculo.marca} {vehiculo.modelo} · {vehiculo.patente}
          </strong>
        </Field>
        <Field label="Precio diario">
          <strong>{formatMoney(vehiculo.precioDiario)}</strong>
        </Field>
        <Field label="Inicio">
          <strong>{formatDateTime(fechaInicio)}</strong>
        </Field>
        <Field label="Finalización">
          <strong>{formatDateTime(fechaFin)}</strong>
        </Field>
        <p className="muted form-note">El importe total se calcula al confirmar, según la duración del alquiler.</p>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancelar
          </button>
          <button type="button" className="btn primary" onClick={() => void confirmar()} disabled={saving}>
            {saving ? 'Reservando…' : 'Confirmar reserva'}
          </button>
        </div>
      </div>
    </Modal>
  )
}
