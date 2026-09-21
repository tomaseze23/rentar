import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { reservasApi } from '../../api/reservas'
import { vehiculosApi } from '../../api/vehiculos'
import { Alert, Badge, ConfirmDialog, Field, Modal, errorMessage } from '../../components/ui'
import { formatMoney } from '../../format'
import { TIPOS_VEHICULO, type TipoVehiculo, type Vehiculo, type VehiculoInput } from '../../types'

type FiltroActivo = 'todos' | 'activos' | 'inactivos'

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

const ESTADO_TONE = { DISPONIBLE: 'green', RESERVADO: 'amber', EN_ALQUILER: 'blue' } as const

function toForm(v: Vehiculo): FormState {
  return {
    patente: v.patente,
    marca: v.marca,
    modelo: v.modelo,
    anio: v.anio,
    color: v.color ?? '',
    tipoVehiculo: v.tipoVehiculo ?? 'SEDAN',
    precioDiario: v.precioDiario === null ? '' : String(v.precioDiario),
  }
}

function validar(form: FormState): string | null {
  if (!form.patente.trim()) return 'La patente es obligatoria.'
  if (!form.marca.trim()) return 'La marca es obligatoria.'
  if (!form.modelo.trim()) return 'El modelo es obligatorio.'
  if (!form.anio.trim()) return 'El año es obligatorio.'
  const precio = Number(form.precioDiario)
  if (!form.precioDiario || Number.isNaN(precio) || precio <= 0) return 'El precio diario debe ser mayor a 0.'
  return null
}

export function VehiculosPage() {
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])
  const [filtro, setFiltro] = useState<FiltroActivo>('todos')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [editando, setEditando] = useState<Vehiculo | 'nuevo' | null>(null)
  const [aBaja, setABaja] = useState<Vehiculo | null>(null)
  const [vigentes, setVigentes] = useState<{ vehiculoId: number; cantidad: number | 'error' } | null>(null)

  const cargar = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const activo = filtro === 'todos' ? undefined : filtro === 'activos'
      setVehiculos(await vehiculosApi.listar(activo))
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [filtro])

  useEffect(() => {
    void cargar()
  }, [cargar])

  // Antes de confirmar la baja se consulta cuántas reservas confirmadas siguen vigentes (en curso o futuras).
  function abrirBaja(v: Vehiculo) {
    setABaja(v)
    setVigentes(null)
    reservasApi
      .contarVigentes({ vehiculoId: String(v.id) })
      .then((cantidad) => setVigentes({ vehiculoId: v.id, cantidad }))
      .catch(() => setVigentes({ vehiculoId: v.id, cantidad: 'error' }))
  }

  async function confirmarBaja() {
    if (!aBaja) return
    setError(null)
    setNotice(null)
    try {
      await vehiculosApi.baja(aBaja.id)
      setNotice(`Vehículo ${aBaja.patente} dado de baja.`)
      await cargar()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setABaja(null)
    }
  }

  async function onGuardado(mensaje: string) {
    setEditando(null)
    setNotice(mensaje)
    await cargar()
  }

  return (
    <section>
      <div className="page-head">
        <h1>Vehículos</h1>
        <div className="actions">
          <select value={filtro} onChange={(e) => setFiltro(e.target.value as FiltroActivo)} aria-label="Filtrar por estado de baja">
            <option value="todos">Todos</option>
            <option value="activos">Solo activos</option>
            <option value="inactivos">Solo inactivos</option>
          </select>
          <button type="button" className="btn primary" onClick={() => setEditando('nuevo')}>
            Nuevo vehículo
          </button>
        </div>
      </div>

      {notice && <Alert kind="success">{notice}</Alert>}
      {error && <Alert>{error}</Alert>}

      {loading ? (
        <p className="muted">Cargando…</p>
      ) : vehiculos.length === 0 ? (
        <p className="muted">No hay vehículos para mostrar.</p>
      ) : (
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
                <th>Estado</th>
                <th>Baja</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {vehiculos.map((v) => (
                <tr key={v.id} className={v.activo === false ? 'row-inactive' : ''}>
                  <td className="mono">{v.patente}</td>
                  <td>
                    {v.marca} {v.modelo}
                  </td>
                  <td>{v.anio}</td>
                  <td>{v.color ?? '—'}</td>
                  <td>{v.tipoVehiculo ?? '—'}</td>
                  <td className="num">{formatMoney(v.precioDiario)}</td>
                  <td>{v.estado ? <Badge tone={ESTADO_TONE[v.estado]}>{v.estado.replace('_', ' ')}</Badge> : '—'}</td>
                  <td>{v.activo === false ? <Badge tone="gray">Inactivo</Badge> : <Badge tone="green">Activo</Badge>}</td>
                  <td className="row-actions">
                    <button type="button" className="btn small" onClick={() => setEditando(v)}>
                      Editar
                    </button>
                    <button type="button" className="btn small danger" disabled={v.activo === false} onClick={() => abrirBaja(v)}>
                      Dar de baja
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {aBaja && (
        <ConfirmDialog
          title="Dar de baja vehículo"
          message={<MensajeBaja vehiculo={aBaja} vigentes={vigentes?.vehiculoId === aBaja.id ? vigentes.cantidad : null} />}
          confirmLabel="Dar de baja"
          onConfirm={confirmarBaja}
          onCancel={() => setABaja(null)}
        />
      )}

      {editando && (
        <VehiculoForm
          vehiculo={editando === 'nuevo' ? null : editando}
          onClose={() => setEditando(null)}
          onSaved={onGuardado}
        />
      )}
    </section>
  )
}

function MensajeBaja({ vehiculo, vigentes }: { vehiculo: Vehiculo; vigentes: number | 'error' | null }) {
  return (
    <>
      <p>
        ¿Dar de baja el vehículo <strong>{vehiculo.patente}</strong> ({vehiculo.marca} {vehiculo.modelo})? Quedará inactivo y no podrá
        alquilarse en nuevas reservas.
      </p>
      {vigentes === null && <p className="muted">Verificando reservas vigentes…</p>}
      {vigentes === 'error' && <Alert kind="warning">No se pudieron verificar las reservas vigentes del vehículo. Podés continuar igual.</Alert>}
      {typeof vigentes === 'number' && vigentes > 0 && (
        <Alert kind="warning">
          <strong>Atención:</strong> este vehículo tiene {vigentes} {vigentes === 1 ? 'reserva confirmada vigente o futura' : 'reservas confirmadas vigentes o futuras'}.{' '}
          {vigentes === 1 ? 'Seguirá vigente' : 'Seguirán vigentes'}: la baja solo impide nuevas reservas.
        </Alert>
      )}
    </>
  )
}

function VehiculoForm({
  vehiculo,
  onClose,
  onSaved,
}: {
  vehiculo: Vehiculo | null
  onClose: () => void
  onSaved: (mensaje: string) => Promise<void>
}) {
  const [form, setForm] = useState<FormState>(vehiculo ? toForm(vehiculo) : FORM_VACIO)
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
      if (vehiculo) {
        await vehiculosApi.modificar(vehiculo.id, input)
        await onSaved(`Vehículo ${vehiculo.patente} actualizado.`)
      } else {
        await vehiculosApi.crear(input)
        await onSaved(`Vehículo ${input.patente} creado en estado DISPONIBLE.`)
      }
    } catch (e) {
      setError(errorMessage(e))
      setSaving(false)
    }
  }

  return (
    <Modal title={vehiculo ? `Editar ${vehiculo.patente}` : 'Nuevo vehículo'} onClose={onClose}>
      <form onSubmit={onSubmit} className="form-grid">
        {error && <Alert>{error}</Alert>}
        <Field label="Patente" hint={vehiculo ? 'La patente no puede modificarse.' : undefined}>
          <input value={form.patente} onChange={(e) => set('patente', e.target.value)} disabled={vehiculo !== null} maxLength={10} />
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
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
