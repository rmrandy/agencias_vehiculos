import { useEffect, useRef } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useToast } from '../context/ToastContext'
import './Gracias.css'

export function Gracias() {
  const [params] = useSearchParams()
  const toast = useToast()
  const orderId = params.get('orderId')
  const orderNumber = params.get('orderNumber') ?? ''
  const didToast = useRef(false)
  useEffect(() => {
    if (orderId && !didToast.current) {
      didToast.current = true
      toast.success('Compra completada: tu pedido ya está registrado.')
    }
  }, [orderId, toast])

  return (
    <div className="gracias-page">
      <div className="gracias-card">
        <div className="gracias-icon" aria-hidden>
          <span className="gracias-check">✓</span>
        </div>
        <p className="gracias-kicker">Compra exitosa</p>
        <h1>¡Gracias por tu compra!</h1>
        <p className="gracias-text">
          Hemos recibido tu pedido. Recibirás actualizaciones por correo cuando haya novedades en el envío.
        </p>
        {orderNumber && (
          <p className="gracias-order-box">
            <span className="gracias-order-label">Número de pedido</span>
            <strong className="gracias-order-number">{orderNumber}</strong>
          </p>
        )}
        {orderId && (
          <div className="gracias-primary-actions">
            <Link to={`/pedidos/${orderId}`} className="btn btn-primary btn-lg">
              Ver detalle y recibos
            </Link>
          </div>
        )}
        <div className="gracias-links">
          <Link to="/pedidos" className="btn btn-secondary">
            Mis pedidos
          </Link>
          <Link to="/tienda" className="btn btn-secondary">
            Seguir comprando
          </Link>
        </div>
      </div>
    </div>
  )
}
