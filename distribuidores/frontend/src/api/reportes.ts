import { apiFetch } from './config'

export interface MasVendidoRow {
  partId: number
  partNumber?: string | null
  partTitle?: string | null
  totalQty: number
  totalImporte: number
}

export interface VentaDiariaRow {
  fecha: string
  pedidoCount: number
  totalImporte: number
}

export interface PedidoEstadoRow {
  estado: string
  cantidadPedidos: number
  totalImporte: number
}

function qs(userId: number, from?: string, to?: string, extra?: Record<string, string | number | undefined>) {
  const p = new URLSearchParams({ userId: String(userId) })
  if (from) p.set('from', from)
  if (to) p.set('to', to)
  if (extra) {
    for (const [k, v] of Object.entries(extra)) {
      if (v !== undefined && v !== '') p.set(k, String(v))
    }
  }
  return p.toString()
}

export async function getMasVendidos(userId: number, from?: string, to?: string, top = 30): Promise<MasVendidoRow[]> {
  return apiFetch(`/api/reportes/mas-vendidos?${qs(userId, from, to, { top })}`)
}

export async function getVentasDiarias(userId: number, from?: string, to?: string): Promise<VentaDiariaRow[]> {
  return apiFetch(`/api/reportes/ventas-diarias?${qs(userId, from, to)}`)
}

export async function getPedidosPorEstado(userId: number, from?: string, to?: string): Promise<PedidoEstadoRow[]> {
  return apiFetch(`/api/reportes/pedidos-por-estado?${qs(userId, from, to)}`)
}

export function defaultReportDateRange(): { from: string; to: string } {
  const to = new Date()
  const from = new Date()
  from.setMonth(from.getMonth() - 3)
  return { from: from.toISOString().slice(0, 10), to: to.toISOString().slice(0, 10) }
}

/** Notifica a la fábrica (engagement); no lanza para no bloquear la UI. */
export async function reportVistoDetalle(partId: number, userId?: number): Promise<void> {
  try {
    await apiFetch('/api/reportes/visto-detalle', {
      method: 'POST',
      body: JSON.stringify({ partId, userId: userId ?? undefined }),
    })
  } catch {
    /* ignorar */
  }
}

export async function reportAgregadoCarrito(partId: number, userId?: number): Promise<void> {
  try {
    await apiFetch('/api/reportes/agregado-carrito', {
      method: 'POST',
      body: JSON.stringify({ partId, userId: userId ?? undefined }),
    })
  } catch {
    /* ignorar */
  }
}
