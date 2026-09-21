import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { clientesApi } from '../../api/clientes'
import { reservasApi } from '../../api/reservas'
import { Alert, Badge, ConfirmDialog, Field, Modal, errorMessage } from '../../components/ui'
import { formatDate } from '../../format'
import type { Cliente } from '../../types'

interface FormState {
  email: string
  password: string
  documento: string
  nombre: string
  apellido: string
  telefono: string
  fechaNacimiento: string
}

const FORM_VACIO: FormState = {
  email: '',
  password: '',
  documento: '',
  nombre: '',
  apellido: '',
  telefono: '',
  fechaNacimiento: '',
}

function toForm(c: Cliente): FormState {
  return {
    email: c.email,
    password: '',
    documento: c.documento,
    nombre: c.nombre,
    apellido: c.apellido,
    telefono: c.telefono ?? '',
    fechaNacimiento: c.fechaNacimiento ?? '',
  }
}

export function ClientesPage() {
  const [clientes, setClientes] = useState<Cliente[]>([])
  const [incluirInactivos, setIncluirInactivos] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [editando, setEditando] = useState<Cliente | 'nuevo' | null>(null)
  const [aBaja, setABaja] = useState<Cliente | null>(null)
  const [vigentes, setVigentes] = useState<{ clienteId: number; cantidad: number | 'error' } | null>(null)

  const cargar = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setClientes(await clientesApi.listar(incluirInactivos))
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }, [incluirInactivos])

  useEffect(() => {
    void cargar()
  }, [cargar])

  // Antes de confirmar la baja se consulta cuántas reservas confirmadas siguen vigentes (en curso o futuras).
  function abrirBaja(c: Cliente) {
    setABaja(c)
    setVigentes(null)
    reservasApi
      .contarVigentes({ clienteId: String(c.id) })
      .then((cantidad) => setVigentes({ clienteId: c.id, cantidad }))
      .catch(() => setVigentes({ clienteId: c.id, cantidad: 'error' }))
  }

  async function confirmarBaja() {
    if (!aBaja) return
    setError(null)
    setNotice(null)
    try {
      await clientesApi.baja(aBaja.id)
      setNotice(`Cliente ${aBaja.nombre} ${aBaja.apellido} dado de baja.`)
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
        <h1>Clientes</h1>
        <div className="actions">
          <label className="check">
            <input type="checkbox" checked={incluirInactivos} onChange={(e) => setIncluirInactivos(e.target.checked)} />
            Incluir inactivos
          </label>
          <button type="button" className="btn primary" onClick={() => setEditando('nuevo')}>
            Nuevo cliente
          </button>
        </div>
      </div>

      {notice && <Alert kind="success">{notice}</Alert>}
      {error && <Alert>{error}</Alert>}

      {loading ? (
        <p className="muted">Cargando…</p>
      ) : clientes.length === 0 ? (
        <p className="muted">No hay clientes para mostrar.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Cliente</th>
                <th>Documento</th>
                <th>Email</th>
                <th>Teléfono</th>
                <th>Nacimiento</th>
                <th>Estado</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {clientes.map((c) => (
                <tr key={c.id} className={c.activo ? '' : 'row-inactive'}>
                  <td>
                    {c.apellido}, {c.nombre}
                  </td>
                  <td className="mono">{c.documento}</td>
                  <td>{c.email}</td>
                  <td>{c.telefono ?? '—'}</td>
                  <td>{formatDate(c.fechaNacimiento)}</td>
                  <td>{c.activo ? <Badge tone="green">Activo</Badge> : <Badge tone="gray">Inactivo</Badge>}</td>
                  <td className="row-actions">
                    <button type="button" className="btn small" disabled={!c.activo} onClick={() => setEditando(c)}>
                      Editar
                    </button>
                    <button type="button" className="btn small danger" disabled={!c.activo} onClick={() => abrirBaja(c)}>
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
          title="Dar de baja cliente"
          message={<MensajeBaja cliente={aBaja} vigentes={vigentes?.clienteId === aBaja.id ? vigentes.cantidad : null} />}
          confirmLabel="Dar de baja"
          onConfirm={confirmarBaja}
          onCancel={() => setABaja(null)}
        />
      )}

      {editando && (
        <ClienteForm cliente={editando === 'nuevo' ? null : editando} onClose={() => setEditando(null)} onSaved={onGuardado} />
      )}
    </section>
  )
}

function MensajeBaja({ cliente, vigentes }: { cliente: Cliente; vigentes: number | 'error' | null }) {
  return (
    <>
      <p>
        ¿Dar de baja a{' '}
        <strong>
          {cliente.nombre} {cliente.apellido}
        </strong>
        ? Su usuario quedará inactivo: no podrá ingresar ni realizar nuevos alquileres.
      </p>
      {vigentes === null && <p className="muted">Verificando reservas vigentes…</p>}
      {vigentes === 'error' && <Alert kind="warning">No se pudieron verificar las reservas vigentes del cliente. Podés continuar igual.</Alert>}
      {typeof vigentes === 'number' && vigentes > 0 && (
        <Alert kind="warning">
          <strong>Atención:</strong> este cliente tiene {vigentes} {vigentes === 1 ? 'reserva confirmada vigente o futura' : 'reservas confirmadas vigentes o futuras'}.{' '}
          {vigentes === 1 ? 'Seguirá vigente' : 'Seguirán vigentes'}, pero al quedar inactivo no podrá ingresar para consultarla{vigentes === 1 ? '' : 's'} ni cancelarla{vigentes === 1 ? '' : 's'}.
        </Alert>
      )}
    </>
  )
}

function ClienteForm({
  cliente,
  onClose,
  onSaved,
}: {
  cliente: Cliente | null
  onClose: () => void
  onSaved: (mensaje: string) => Promise<void>
}) {
  const [form, setForm] = useState<FormState>(cliente ? toForm(cliente) : FORM_VACIO)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const set = (key: keyof FormState, value: string) => setForm((f) => ({ ...f, [key]: value }))

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!form.nombre.trim() || !form.apellido.trim()) {
      setError('Nombre y apellido son obligatorios.')
      return
    }
    if (!cliente && (!form.email.trim() || !form.password || !form.documento.trim())) {
      setError('Email, contraseña y documento son obligatorios.')
      return
    }
    setSaving(true)
    setError(null)
    try {
      const comunes = {
        nombre: form.nombre.trim(),
        apellido: form.apellido.trim(),
        telefono: form.telefono.trim() || null,
        fechaNacimiento: form.fechaNacimiento || null,
      }
      if (cliente) {
        await clientesApi.actualizar(cliente.id, comunes)
        await onSaved(`Cliente ${comunes.nombre} ${comunes.apellido} actualizado.`)
      } else {
        await clientesApi.crear({
          ...comunes,
          email: form.email.trim(),
          password: form.password,
          documento: form.documento.trim(),
        })
        await onSaved(`Cliente ${comunes.nombre} ${comunes.apellido} creado.`)
      }
    } catch (e) {
      setError(errorMessage(e))
      setSaving(false)
    }
  }

  return (
    <Modal title={cliente ? `Editar ${cliente.nombre} ${cliente.apellido}` : 'Nuevo cliente'} onClose={onClose}>
      <form onSubmit={onSubmit} className="form-grid">
        {error && <Alert>{error}</Alert>}
        <Field label="Nombre">
          <input value={form.nombre} onChange={(e) => set('nombre', e.target.value)} />
        </Field>
        <Field label="Apellido">
          <input value={form.apellido} onChange={(e) => set('apellido', e.target.value)} />
        </Field>
        <Field label="Email" hint={cliente ? 'El email no puede modificarse.' : 'Será su usuario de acceso.'}>
          <input type="email" value={form.email} onChange={(e) => set('email', e.target.value)} disabled={cliente !== null} />
        </Field>
        <Field label="Documento" hint={cliente ? 'El documento no puede modificarse.' : undefined}>
          <input value={form.documento} onChange={(e) => set('documento', e.target.value)} disabled={cliente !== null} />
        </Field>
        {!cliente && (
          <Field label="Contraseña inicial">
            <input type="password" value={form.password} onChange={(e) => set('password', e.target.value)} autoComplete="new-password" />
          </Field>
        )}
        <Field label="Teléfono (opcional)">
          <input value={form.telefono} onChange={(e) => set('telefono', e.target.value)} />
        </Field>
        <Field label="Fecha de nacimiento (opcional)">
          <input type="date" value={form.fechaNacimiento} onChange={(e) => set('fechaNacimiento', e.target.value)} />
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
