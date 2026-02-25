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
