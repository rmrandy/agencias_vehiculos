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
      toast.success('Pedido realizado correctamente')
    }
  }, [orderId, toast])

  return (
    <div className="gracias-page">
      <div className="gracias-card">
        <div className="gracias-icon">✓</div>
        <h1>¡Gracias por tu compra!</h1>
        <p className="gracias-text">
          Tu pedido ha sido registrado correctamente.
          {orderNumber && (
            <>
              <br />
              <strong>Número de pedido: {orderNumber}</strong>
            </>
          )}
        </p>
        {orderId && (
          <Link to={`/pedidos/${orderId}`} className="btn btn-primary btn-lg">
            Ver detalle del pedido
          </Link>
        )}
        <div className="gracias-links">
          <Link to="/pedidos" className="btn btn-secondary">Mis pedidos</Link>
          <Link to="/tienda" className="btn btn-secondary">Seguir comprando</Link>
        </div>
      </div>
    </div>
  )
}
