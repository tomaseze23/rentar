import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { vehiculosExternosApi } from '../../api/vehiculosExternos'
import { Alert, Badge, Field, Modal, errorMessage } from '../../components/ui'
import { formatMoney } from '../../format'
import {
  TIPOS_VEHICULO,
  type EstadoSolicitud,
  type ResultadoValidacion,
  type SolicitudIncorporacion,
  type TipoVehiculo,
  type VehiculoInput,
} from '../../types'

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

interface FormState {
  patente: string
  marca: string
  modelo: string
  anio: string
  color: string
  tipoVehiculo: TipoVehiculo
  precioDiario: string
}

const FORM_VACIO: FormState = {
  patente: '',
  marca: '',
  modelo: '',
  anio: '',
  color: '',
  tipoVehiculo: 'SEDAN',
  precioDiario: '',
}

function validar(form: FormState): string | null {
  if (!form.patente.trim()) return 'La patente es obligatoria.'
  if (!form.marca.trim()) return 'La marca es obligatoria.'
  if (!form.modelo.trim()) return 'El modelo es obligatorio.'
  if (!/^\d{4}$/.test(form.anio.trim())) return 'El año debe tener 4 dígitos.'
  const precio = Number(form.precioDiario)
  if (!form.precioDiario || Number.isNaN(precio) || precio <= 0) return 'El precio diario debe ser mayor a 0.'
  return null
}

function motivo(s: SolicitudIncorporacion): string | null {
  if (s.estado !== 'RECHAZADA') return null
  if (s.validacionPatente === 'RECHAZADO') return 'La patente figura denunciada.'
  if (s.validacionTitularidad === 'RECHAZADO') return 'La titularidad no coincide con tu cuenta.'
  return 'La solicitud fue rechazada.'
}

export function MisVehiculosPage() {
  const [items, setItems] = useState<SolicitudIncorporacion[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [proponiendo, setProponiendo] = useState(false)

  const cargar = useCallback(async () => {
    try {
      setItems(await vehiculosExternosApi.listar())
      setError(null)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void cargar()
  }, [cargar])

  // Las validaciones son asincrónicas: mientras haya solicitudes pendientes se refresca solo.
  const hayPendientes = items.some((s) => s.estado === 'PENDIENTE')
  useEffect(() => {
    if (!hayPendientes) return
    const timer = setInterval(() => void cargar(), 3000)
    return () => clearInterval(timer)
  }, [hayPendientes, cargar])

  async function onEnviada(patente: string) {
    setProponiendo(false)
    setNotice(`Solicitud para ${patente} enviada. Se están validando la patente y la titularidad.`)
    await cargar()
  }

  return (
    <section>
      <div className="page-head">
        <h1>Mis vehículos</h1>
        <div className="actions">
          <button type="button" className="btn primary" onClick={() => setProponiendo(true)}>
            Proponer vehículo
          </button>
        </div>
      </div>
      <p className="muted">
        Proponé un vehículo propio para incorporarlo a la flota. Se valida la patente y que figure a tu nombre.
      </p>

      {notice && <Alert kind="success">{notice}</Alert>}
      {error && <Alert>{error}</Alert>}

      {loading ? (
        <p className="muted">Cargando…</p>
      ) : items.length === 0 ? (
        <p className="muted">Todavía no propusiste ningún vehículo.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Patente</th>
                <th>Vehículo</th>
                <th className="num">Precio diario</th>
                <th>Validación de patente</th>
                <th>Validación de titularidad</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {items.map((s) => (
                <tr key={s.id}>
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
                    {motivo(s) && <small className="muted"> {motivo(s)}</small>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {proponiendo && <SolicitudForm onClose={() => setProponiendo(false)} onSent={onEnviada} />}
    </section>
  )
}

function SolicitudForm({ onClose, onSent }: { onClose: () => void; onSent: (patente: string) => Promise<void> }) {
  const [form, setForm] = useState<FormState>(FORM_VACIO)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) => setForm((f) => ({ ...f, [key]: value }))

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    const problema = validar(form)
    if (problema) {
      setError(problema)
      return
    }
    const input: VehiculoInput = {
      patente: form.patente.trim().toUpperCase(),
      marca: form.marca.trim(),
      modelo: form.modelo.trim(),
      anio: form.anio.trim(),
      color: form.color.trim() || null,
      tipoVehiculo: form.tipoVehiculo,
      precioDiario: Number(form.precioDiario),
    }
    setSaving(true)
    setError(null)
    try {
      await vehiculosExternosApi.proponer(input)
      await onSent(input.patente)
    } catch (e) {
      setError(errorMessage(e))
      setSaving(false)
    }
  }

  return (
    <Modal title="Proponer vehículo" onClose={onClose}>
      <form onSubmit={onSubmit} className="form-grid">
        {error && <Alert>{error}</Alert>}
        <Field label="Patente" hint="Debe figurar a tu nombre en el registro de titularidad.">
          <input value={form.patente} onChange={(e) => set('patente', e.target.value)} maxLength={10} />
        </Field>
        <Field label="Año">
          <input value={form.anio} onChange={(e) => set('anio', e.target.value)} inputMode="numeric" maxLength={4} />
        </Field>
        <Field label="Marca">
          <input value={form.marca} onChange={(e) => set('marca', e.target.value)} />
        </Field>
        <Field label="Modelo">
          <input value={form.modelo} onChange={(e) => set('modelo', e.target.value)} />
        </Field>
        <Field label="Color (opcional)">
          <input value={form.color} onChange={(e) => set('color', e.target.value)} />
        </Field>
        <Field label="Tipo de vehículo">
          <select value={form.tipoVehiculo} onChange={(e) => set('tipoVehiculo', e.target.value as TipoVehiculo)}>
            {TIPOS_VEHICULO.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Precio diario (ARS)">
          <input value={form.precioDiario} onChange={(e) => set('precioDiario', e.target.value)} inputMode="decimal" />
        </Field>
        <div className="form-actions">
          <button type="button" className="btn" onClick={onClose}>
            Cancelar
          </button>
          <button type="submit" className="btn primary" disabled={saving}>
            {saving ? 'Enviando…' : 'Enviar solicitud'}
          </button>
        </div>
      </form>
    </Modal>
  )
}