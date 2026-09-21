const money = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })

export const formatMoney = (value: number | string | null | undefined) =>
  value === null || value === undefined || value === '' ? '—' : money.format(Number(value))

export const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' })

// Valor para <input type="datetime-local"> (hora local, sin segundos).
export function toDateTimeInput(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export const formatDate =(iso: string | null | undefined) =>
  iso ? new Date(`${iso}T00:00:00`).toLocaleDateString('es-AR') : '—'
