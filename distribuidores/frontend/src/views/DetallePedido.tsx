import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getPedido } from '../api/pedidos'
import { LoadingModal } from '../components/LoadingModal'
import './DetallePedido.css'

const API_IMAGES = (import.meta.env.VITE_API_URL || 'http://localhost:5080').replace(/\/$/, '')

function formatDate(iso: string) {
  try {
    const d = new Date(iso)
    return d.toLocaleDateString('es', { dateStyle: 'medium' }) + ' ' + d.toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' })
  } catch {
    return iso
  }
}

export function DetallePedido() {
  const { orderId } = useParams<{ orderId: string }>()
  const [data, setData] = useState<{
    order: { orderNumber: string; subtotal: number; shippingTotal: number; total: number; orderType: string; createdAt: string }
    items: { partId: number; partTitle?: string; qty: number; unitPrice: number; lineTotal: number }[]
    status: { status: string; trackingNumber?: string; etaDays?: number }
  } | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!orderId) {
      setLoading(false)
      return
    }
    getPedido(Number(orderId))
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false))
  }, [orderId])

  if (loading) return <div className="detalle-pedido-page"><LoadingModal open message="Cargando pedido..." /></div>
  if (!data) return <div className="detalle-pedido-page"><p>Pedido no encontrado.</p></div>

  const { order, items, status } = data

  const statusLabel: Record<string, string> = {
    INITIATED: 'Iniciado',
    CONFIRMED: 'Confirmado',
    PREPARING: 'En preparación',
    SHIPPED: 'Enviado',
    DELIVERED: 'Entregado',
    CANCELLED: 'Cancelado',
  }
  const statusText = statusLabel[status.status] ?? status.status

  return (
    <div className="detalle-pedido-page">
      <div className="detalle-pedido-header detalle-pedido-card">
        <h1>Pedido {order.orderNumber}</h1>
        <p className="detalle-pedido-date">{formatDate(order.createdAt)}</p>
        <p className="order-type">{order.orderType === 'ENTERPRISE_API' ? 'Pedido empresarial' : 'Pedido web'}</p>
        <div className="detalle-pedido-status">
          <span className="status-badge">{statusText}</span>
          {status.trackingNumber && <span className="tracking">Tracking: {status.trackingNumber}</span>}
          {status.etaDays != null && <span className="eta">Entrega estimada: {status.etaDays} días</span>}
        </div>
      </div>

      <section className="detalle-pedido-items">
        <h2>Productos</h2>
        <ul className="detalle-items">
          {items.map((item, i) => (
            <li key={i} className="detalle-item-row">
              <span className="item-name">{item.partTitle ?? `Repuesto #${item.partId}`}</span>
              <span className="item-qty">× {item.qty}</span>
              <span className="item-price">${Number(item.unitPrice).toFixed(2)} c/u</span>
              <span className="item-total">${Number(item.lineTotal).toFixed(2)}</span>
            </li>
          ))}
        </ul>
      </section>

      <section className="detalle-pedido-totals">
        <div className="totals-row">
          <span>Subtotal</span>
          <span>${Number(order.subtotal).toFixed(2)}</span>
        </div>
        {order.shippingTotal != null && Number(order.shippingTotal) > 0 && (
          <div className="totals-row">
            <span>Envío</span>
            <span>${Number(order.shippingTotal).toFixed(2)}</span>
          </div>
        )}
        <div className="totals-row total">
          <span>Total</span>
          <span>${Number(order.total).toFixed(2)}</span>
        </div>
      </section>

      <div className="detalle-pedido-actions">
        <Link to="/pedidos" className="btn btn-secondary">Mis pedidos</Link>
        <Link to="/tienda" className="btn btn-primary">Seguir comprando</Link>
      </div>
    </div>
  )
}
