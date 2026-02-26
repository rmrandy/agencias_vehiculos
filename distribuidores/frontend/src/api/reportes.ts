import { apiFetch } from './config'

/**
 * Notifica a la fábrica que se vio el detalle del repuesto (vía backend distribuidores).
 */
export async function reportVistoDetalle(partId: number, userId?: number): Promise<void> {
  try {
    await apiFetch('/api/reportes/visto-detalle', {
      method: 'POST',
      body: JSON.stringify({ partId, userId: userId ?? null }),
    })
  } catch {
    // No bloquear la UI si falla el reporte
  }
}

/**
 * Notifica a la fábrica que se agregó el repuesto al carrito (vía backend distribuidores).
 */
export async function reportAgregadoCarrito(partId: number, userId?: number): Promise<void> {
  try {
    await apiFetch('/api/reportes/agregado-carrito', {
      method: 'POST',
      body: JSON.stringify({ partId, userId: userId ?? null }),
    })
  } catch {
    // No bloquear la UI si falla el reporte
  }
}
