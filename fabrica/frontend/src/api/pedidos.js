import { apiFetch } from './config'

/**
 * Crea un pedido. Si se pasa payment (simulación de pasarela), se valida tarjeta y se envía correo al comprador.
 * @param {number} userId
 * @param {Array<{ partId: number, qty: number }>} items
 * @param {{ cardNumber: string, expiryMonth: number, expiryYear: number }} payment - opcional
 */
export async function createOrder(userId, items, payment = null) {
  const body = { userId, items }
  if (payment) {
    body.payment = {
      cardNumber: payment.cardNumber.replace(/\D/g, ''),
      expiryMonth: payment.expiryMonth,
      expiryYear: payment.expiryYear
    }
  }
  return apiFetch('/api/pedidos', {
    method: 'POST',
    body: JSON.stringify(body)
  })
}

export async function getUserOrders(userId) {
  return apiFetch(`/api/pedidos/usuario/${userId}`)
}

export async function getOrderById(orderId) {
  return apiFetch(`/api/pedidos/${orderId}`)
}

export async function getOrderHistory(orderId) {
  return apiFetch(`/api/pedidos/${orderId}/historial`)
}

export async function updateOrderStatus(orderId, status, comment, changedByUserId, trackingNumber = null, etaDays = null) {
  const body = { status, comment, changedByUserId }
  if (trackingNumber != null) body.trackingNumber = trackingNumber
  if (etaDays != null) body.etaDays = etaDays
  return apiFetch(`/api/pedidos/${orderId}/estado`, {
    method: 'PUT',
    body: JSON.stringify(body)
  })
}

/**
 * Lista todos los pedidos (admin). Opcionalmente con filtros.
 * @returns {Array<{ order: Object, latestStatus: Object }>}
 */
export async function getAllOrders(filters = {}) {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.userId) params.set('userId', filters.userId)
  if (filters.from) params.set('from', filters.from)
  if (filters.to) params.set('to', filters.to)
  const qs = params.toString()
  return apiFetch('/api/pedidos' + (qs ? '?' + qs : ''))
}

/** Estados válidos del flujo de pedidos */
export async function getOrderStatusFlow() {
  return apiFetch('/api/pedidos/flujo-estados')
}
