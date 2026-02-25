import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getPedidosTodos, updatePedidoEstado, type OrderHeader } from '../api/pedidos'
import { LoadingModal } from '../components/LoadingModal'
import { useToast } from '../context/ToastContext'
import './GestionPedidos.css'

const STATUS_OPTIONS = [
  { value: 'INITIATED', label: 'Iniciado' },
  { value: 'CONFIRMED', label: 'Confirmado' },
  { value: 'PREPARING', label: 'En preparación' },
  { value: 'SHIPPED', label: 'Enviado' },
  { value: 'DELIVERED', label: 'Entregado' },
  { value: 'CANCELLED', label: 'Cancelado' },
]

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleDateString('es', { dateStyle: 'short' }) + ' ' + new Date(iso).toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' })
  } catch {
    return iso
  }
}

export function GestionPedidos() {
  const { user, isLoggedIn } = useAuth()
  const toast = useToast()
  const [orders, setOrders] = useState<OrderHeader[]>([])
  const [loading, setLoading] = useState(true)
  const [updatingId, setUpdatingId] = useState<number | null>(null)
  const [modalOrderId, setModalOrderId] = useState<number | null>(null)
  const [formStatus, setFormStatus] = useState('CONFIRMED')
  const [formComment, setFormComment] = useState('')
  const [formTracking, setFormTracking] = useState('')
  const [formEta, setFormEta] = useState('')

  const canManage = isLoggedIn && user && (user.roles?.includes('ADMIN') || user.roles?.includes('EMPLOYEE'))

  useEffect(() => {
    if (!canManage || !user) {
      setLoading(false)
      return
    }
    getPedidosTodos(user.userId)
      .then(setOrders)
      .catch(() => {
        setOrders([])
        toast.error('No tienes permiso o hubo un error')
      })
      .finally(() => setLoading(false))
  }, [canManage, user])

  async function handleUpdateEstado() {
    if (!user || !modalOrderId) return
    setUpdatingId(modalOrderId)
    try {
      await updatePedidoEstado(modalOrderId, {
        userId: user.userId,
        status: formStatus,
        comment: formComment || undefined,
        trackingNumber: formTracking || undefined,
        etaDays: formEta ? parseInt(formEta, 10) : undefined,
      })
      toast.success('Estado actualizado')
      setModalOrderId(null)
      setFormComment('')
      setFormTracking('')
      setFormEta('')
      getPedidosTodos(user.userId).then(setOrders)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Error al actualizar')
    } finally {
      setUpdatingId(null)
    }
  }

  if (!isLoggedIn || !user) {
    return (
      <div className="gestion-pedidos-page">
        <p>Inicia sesión para acceder.</p>
      </div>
    )
  }

  if (!canManage) {
    return (
      <div className="gestion-pedidos-page">
        <p>No tienes permiso para gestionar pedidos (requiere rol Admin o Empleado).</p>
      </div>
    )
  }

  return (
    <div className="gestion-pedidos-page">
      <LoadingModal open={loading} message="Cargando pedidos..." />
      <header className="gestion-pedidos-header">
        <h1>Gestión de pedidos</h1>
        <p className="page-subtitle">Actualiza el estado de los pedidos (Admin / Empleado).</p>
      </header>
      {!loading && orders.length === 0 && <p className="gestion-pedidos-empty">No hay pedidos.</p>}
      {!loading && orders.length > 0 && (
        <div className="gestion-pedidos-table-wrap">
          <table className="gestion-pedidos-table">
            <thead>
              <tr>
                <th>Nº pedido</th>
                <th>Fecha</th>
                <th>Tipo</th>
                <th>Total</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((o) => (
                <tr key={o.orderId}>
                  <td>
                    <Link to={`/pedidos/${o.orderId}`} className="order-link">
                      {o.orderNumber}
                    </Link>
                  </td>
                  <td>{formatDate(o.createdAt)}</td>
                  <td>{o.orderType === 'ENTERPRISE_API' ? 'Empresarial' : 'Web'}</td>
                  <td className="total-cell">${Number(o.total).toFixed(2)}</td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-sm btn-primary"
                      onClick={() => {
                        setModalOrderId(o.orderId)
                        setFormStatus('CONFIRMED')
                        setFormComment('')
                        setFormTracking('')
                        setFormEta('')
                      }}
                    >
                      Actualizar estado
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {modalOrderId != null && (
        <div className="modal-overlay" onClick={() => setModalOrderId(null)}>
          <div className="modal-content gestion-pedidos-modal" onClick={(e) => e.stopPropagation()}>
            <h2>Actualizar estado del pedido</h2>
            <div className="form-row">
              <label>Estado</label>
              <select value={formStatus} onChange={(e) => setFormStatus(e.target.value)}>
                {STATUS_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>
            <div className="form-row">
              <label>Comentario (opcional)</label>
              <input type="text" value={formComment} onChange={(e) => setFormComment(e.target.value)} placeholder="Comentario" />
            </div>
            <div className="form-row">
              <label>Nº seguimiento (opcional)</label>
              <input type="text" value={formTracking} onChange={(e) => setFormTracking(e.target.value)} placeholder="Tracking" />
            </div>
            <div className="form-row">
              <label>Días hasta entrega (opcional)</label>
              <input type="number" min={0} value={formEta} onChange={(e) => setFormEta(e.target.value)} placeholder="Ej. 5" />
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setModalOrderId(null)}>Cancelar</button>
              <button type="button" className="btn btn-primary" disabled={updatingId !== null} onClick={handleUpdateEstado}>
                {updatingId !== null ? 'Guardando…' : 'Guardar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
