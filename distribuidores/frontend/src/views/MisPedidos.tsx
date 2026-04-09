import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCurrency } from '../context/CurrencyContext'
import { getPedidosByUser, getPedidoReciboPdfUrl, type OrderListRow } from '../api/pedidos'
import { LoadingModal } from '../components/LoadingModal'
import './MisPedidos.css'

const STATUS_LABEL: Record<string, string> = {
  INITIATED: 'Iniciado',
  CONFIRMED: 'Confirmado',
  PREPARING: 'En preparación',
  IN_PREPARATION: 'En preparación',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado',
}

export function MisPedidos() {
  const { user, isLoggedIn } = useAuth()
  const { formatOrder } = useCurrency()
  const [orders, setOrders] = useState<OrderListRow[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!isLoggedIn || !user) {
      setLoading(false)
      return
    }
    getPedidosByUser(user.userId)
      .then(setOrders)
      .catch(() => setOrders([]))
      .finally(() => setLoading(false))
  }, [isLoggedIn, user])

  if (!isLoggedIn) {
    return (
      <div className="mis-pedidos-page">
        <p>Inicia sesión para ver tus pedidos.</p>
      </div>
    )
  }

  return (
    <div className="mis-pedidos-page">
      <LoadingModal open={loading} message="Cargando pedidos..." />
      <h1>Mis pedidos</h1>
      <p className="mis-pedidos-sub">Consulta el estado, el detalle y descarga el recibo en PDF.</p>

      {!loading && orders.length === 0 && <p>No tienes pedidos.</p>}

      {!loading && orders.length > 0 && (
        <ul className="pedidos-list">
          {orders.map((order) => {
            const fab = order.fabricaStatuses?.[0]
            const fromFabrica = Boolean(fab?.status)
            const effectiveStatus = (fromFabrica ? fab?.status : order.status) ?? 'INITIATED'
            const statusKey = String(effectiveStatus).toUpperCase()
            const statusText = STATUS_LABEL[statusKey] ?? effectiveStatus ?? '—'
            const lines = order.lineCount ?? 0
            const trackingFab = fab?.trackingNumber
            const trackingLocal = order.trackingNumber
            const tracking = trackingFab || trackingLocal
            return (
              <li key={order.orderId} className="pedido-card">
                <div className="pedido-card-main">
                  <Link to={`/pedidos/${order.orderId}`} className="pedido-number">
                    {order.orderNumber}
                  </Link>
                  <div className="pedido-meta">
                    <span className={`pedido-status pedido-status--${statusKey.toLowerCase()}`}>{statusText}</span>
                    {fromFabrica && (
                      <span className="pedido-sync-badge" title="Estado consultado al API de la fábrica">
                        Fábrica
                      </span>
                    )}
                    <span className="pedido-lines">
                      {lines} {lines === 1 ? 'línea' : 'líneas'}
                    </span>
                    <span className="pedido-channel">
                      {order.orderType === 'ENTERPRISE_API' ? 'Empresarial' : 'Web'}
                    </span>
                  </div>
                  {tracking && (
                    <p className="pedido-tracking">Seguimiento: {tracking}</p>
                  )}
                </div>
                <div className="pedido-card-side">
                  <span className="pedido-total">
                    {formatOrder(Number(order.total), order.currency ?? 'USD')}
                  </span>
                  <div className="pedido-actions">
                    <Link to={`/pedidos/${order.orderId}`} className="btn btn-sm btn-secondary">
                      Ver detalle
                    </Link>
                    <a
                      href={getPedidoReciboPdfUrl(order.orderId)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn btn-sm btn-primary"
                    >
                      PDF
                    </a>
                  </div>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
