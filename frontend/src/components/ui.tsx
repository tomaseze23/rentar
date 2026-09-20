import { useEffect, useState, type ReactNode } from 'react'

export function Alert({
  kind = 'error',
  children,
}: {
  kind?: 'error' | 'success' | 'info' | 'warning'
  children: ReactNode
}) {
  return (
    <div className={`alert ${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}

export function Badge({
  tone,
  children,
}: {
  tone: 'green' | 'gray' | 'red' | 'blue' | 'amber'
  children: ReactNode
}) {
  return <span className={`badge ${tone}`}>{children}</span>
}

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  )
}

export function Modal({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="overlay" onMouseDown={onClose}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h2>{title}</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Cerrar">
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function ConfirmDialog({
  title,
  message,
  confirmLabel,
  onConfirm,
  onCancel,
}: {
  title: string
  message: ReactNode
  confirmLabel: string
  onConfirm: () => Promise<void>
  onCancel: () => void
}) {
  const [busy, setBusy] = useState(false)

  async function confirmar() {
    setBusy(true)
    try {
      await onConfirm()
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title={title} onClose={busy ? () => {} : onCancel}>
      <div className="confirm-message">{message}</div>
      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel} disabled={busy}>
          Volver
        </button>
        <button type="button" className="btn danger-solid" onClick={() => void confirmar()} disabled={busy}>
          {busy ? 'Procesando…' : confirmLabel}
        </button>
      </div>
    </Modal>
  )
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Ocurrió un error inesperado.'
}
