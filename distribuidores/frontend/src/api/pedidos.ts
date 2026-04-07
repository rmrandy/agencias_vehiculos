import { apiFetch, API_URL } from './config'

export interface OrderHeader {
  orderId: number
  orderNumber: string
  userId: number
  orderType: string
  subtotal: number
  shippingTotal: number
  total: number
  createdAt: string
}

/** Estado leído en tiempo real desde el API de la fábrica (sincronización). */
export interface FabricaPedidoStatusRow {
  proveedorId?: number
  proveedorNombre?: string
  fabricaOrderId?: number
  status?: string | null
  trackingNumber?: string | null
  etaDays?: number | null
}

/** Respuesta de GET /api/pedidos/usuario/{userId} (incluye estado e ítems). */
export interface OrderListRow extends OrderHeader {
  lineCount?: number
  status?: string
  trackingNumber?: string
  etaDays?: number
  fabricaStatuses?: FabricaPedidoStatusRow[]
}

export interface PaymentParams {
  cardNumber: string
  expiryMonth: number
  expiryYear: number
}

export type PedidoItemPayload =
  | { source: 'local'; partId: number; qty: number }
  | {
      source: 'fabrica'
      proveedorId: number
      fabricaPartId: number
      qty: number
      unitPrice: number
      title?: string
      partNumber?: string
    }

export async function createPedido(
  userId: number,
  items: PedidoItemPayload[],
  payment?: PaymentParams | null
): Promise<OrderHeader> {
  const body: { userId: number; items: PedidoItemPayload[]; payment?: PaymentParams } = {
    userId,
    items,
  }
  if (payment) {
    body.payment = {
      cardNumber: payment.cardNumber.replace(/\D/g, ''),
      expiryMonth: payment.expiryMonth,
      expiryYear: payment.expiryYear,
    }
  }
  return apiFetch('/api/pedidos', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export async function getPedidosByUser(userId: number): Promise<OrderListRow[]> {
  return apiFetch(`/api/pedidos/usuario/${userId}`)
}

/** Descarga del recibo PDF generado en el backend de la distribuidora. */
export function getPedidoReciboPdfUrl(orderId: number): string {
  return `${API_URL}/api/pedidos/${orderId}/recibo`
}

export async function getPedido(orderId: number): Promise<{
  order: OrderHeader
  items: {
    lineSource?: string
    partId?: number | null
    fabricaPartId?: number | null
    proveedorId?: number | null
    fabricaOrderId?: number | null
    fabricaBaseUrl?: string | null
    partNumber?: string | null
    partTitle?: string
    qty: number
    unitPrice: number
    lineTotal: number
    fabricaRemoteStatus?: {
      status?: string | null
      trackingNumber?: string | null
      etaDays?: number | null
    } | null
  }[]
  status: { status: string; trackingNumber?: string; etaDays?: number }
  fabricaStatuses?: FabricaPedidoStatusRow[]
}> {
  return apiFetch(`/api/pedidos/${orderId}`)
}

/** Listar todos los pedidos (requiere ADMIN o EMPLOYEE). */
export async function getPedidosTodos(adminUserId: number): Promise<OrderHeader[]> {
  return apiFetch(`/api/pedidos/todos?userId=${adminUserId}`)
}

/** Actualizar estado del pedido (requiere ADMIN o EMPLOYEE). */
export async function updatePedidoEstado(
  orderId: number,
  body: { userId: number; status: string; comment?: string; trackingNumber?: string; etaDays?: number }
): Promise<{ status: string; trackingNumber?: string; etaDays?: number }> {
  return apiFetch(`/api/pedidos/${orderId}/estado`, {
    method: 'PATCH',
    body: JSON.stringify(body),
  })
}
