import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getPedidosByUser, type OrderHeader } from '../api/pedidos'
import { LoadingModal } from '../components/LoadingModal'
import './MisPedidos.css'

export function MisPedidos() {
  const { user, isLoggedIn } = useAuth()
  const [orders, setOrders] = useState<OrderHeader[]>([])
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
      {!loading && orders.length === 0 && <p>No tienes pedidos.</p>}
      {!loading && orders.length > 0 && (
        <ul className="pedidos-list">
          {orders.map((order) => (
            <li key={order.orderId} className="pedido-item">
              <Link to={`/pedidos/${order.orderId}`}>
                <strong>{order.orderNumber}</strong>
                <span>Total: ${Number(order.total).toFixed(2)}</span>
                <span>{order.orderType === 'ENTERPRISE_API' ? 'Empresarial' : 'Web'}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
