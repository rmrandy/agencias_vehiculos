import { apiFetch } from './config'

/**
 * Log de operaciones de import/export.
 * @param { { limit?: number, operation?: string } } params
 */
export async function getLogImportExport(params = {}) {
  const q = new URLSearchParams()
  if (params.limit != null) q.set('limit', params.limit)
  if (params.operation) q.set('operation', params.operation)
  const qs = q.toString()
  return apiFetch(`/api/reporteria/import-export${qs ? '?' + qs : ''}`)
}

/**
 * Log de altas de inventario.
 * @param { { limit?: number, partId?: number, userId?: number } } params
 */
export async function getLogInventario(params = {}) {
  const q = new URLSearchParams()
  if (params.limit != null) q.set('limit', params.limit)
  if (params.partId != null) q.set('partId', params.partId)
  if (params.userId != null) q.set('userId', params.userId)
  const qs = q.toString()
  return apiFetch(`/api/reporteria/inventario${qs ? '?' + qs : ''}`)
}

function reportQs(from, to, extra = {}) {
  const q = new URLSearchParams()
  if (from) q.set('from', from)
  if (to) q.set('to', to)
  for (const [k, v] of Object.entries(extra)) {
    if (v != null && v !== '') q.set(k, String(v))
  }
  const s = q.toString()
  return s ? `?${s}` : ''
}

/** Rango por defecto: últimos 3 meses hasta hoy (yyyy-MM-dd). */
export function defaultReportDateRange() {
  const to = new Date()
  const from = new Date()
  from.setMonth(from.getMonth() - 3)
  return { from: from.toISOString().slice(0, 10), to: to.toISOString().slice(0, 10) }
}

/** Repuestos más vendidos (pedidos no cancelados). */
export async function getMasVendidos({ from, to, limit = 30 } = {}) {
  return apiFetch(`/api/reporteria/mas-vendidos${reportQs(from, to, { limit })}`)
}

/** Pedidos por día (conteo e importe), sin cancelados. */
export async function getVentasDiarias({ from, to } = {}) {
  return apiFetch(`/api/reporteria/ventas-diarias${reportQs(from, to)}`)
}

/** Pedidos por canal (FABRICA_WEB vs DISTRIBUIDORA). */
export async function getPedidosPorOrigen({ from, to } = {}) {
  return apiFetch(`/api/reporteria/pedidos-por-origen${reportQs(from, to)}`)
}
