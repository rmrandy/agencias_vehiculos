import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { createPedido } from '../api/pedidos'
import { LoadingModal } from '../components/LoadingModal'
import './Checkout.css'

export function Checkout() {
  const { user, isLoggedIn } = useAuth()
  const { items, clear } = useCart()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [cardNumber, setCardNumber] = useState('')
  const [expiry, setExpiry] = useState('')

  useEffect(() => {
    if (!isLoggedIn || !user) navigate('/login')
    else if (items.length === 0) navigate('/carrito')
  }, [isLoggedIn, user, items.length, navigate])

  const orderItems = items.map((x) => ({ partId: x.part.partId, qty: x.qty }))
  const subtotal = items.reduce((s, x) => s + x.part.price * x.qty, 0)
  const digitsOnly = cardNumber.replace(/\D/g, '')
  const hasPayment = digitsOnly.length >= 13 && expiry.length >= 4
  const parseExpiry = () => {
    const raw = expiry.replace(/\D/g, '')
    if (raw.length < 4) return null
    const m = raw.slice(0, 2)
    const y = raw.slice(2, 4)
    const month = parseInt(m, 10)
    let year = parseInt(y, 10)
    if (year < 100) year += 2000
    if (month < 1 || month > 12) return null
    return { month, year }
  }

  if (!isLoggedIn || !user || items.length === 0) {
    return <LoadingModal open message="Redirigiendo..." />
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const exp = parseExpiry()
      const payment = hasPayment && exp
        ? {
            cardNumber: digitsOnly,
            expiryMonth: exp.month,
            expiryYear: exp.year,
          }
        : undefined
      const order = await createPedido(user.userId, orderItems, payment)
      clear()
      navigate(`/gracias?orderId=${order.orderId}&orderNumber=${encodeURIComponent(order.orderNumber)}`, { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al crear el pedido')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="checkout-page">
      <LoadingModal open={loading} message="Procesando pedido..." />
      <h1>Confirmar pedido</h1>
      <p className="checkout-user">Usuario: {user.email}</p>
      {error && <div className="checkout-error">{error}</div>}
      <div className="checkout-summary">
        <strong>Resumen</strong>
        <ul>
          {items.map(({ part, qty }) => (
            <li key={part.partId}>
              {part.title} × {qty} = ${(part.price * qty).toFixed(2)}
            </li>
          ))}
        </ul>
        <p className="total">Total: ${subtotal.toFixed(2)}</p>
      </div>
      <form onSubmit={handleSubmit} className="checkout-form">
        <section className="checkout-payment">
          <h2>Datos de tarjeta (opcional)</h2>
          <p className="checkout-payment-hint">Simulación: se valida formato y vencimiento.</p>
          <div className="payment-card-fields">
            <div className="form-row">
              <label>Número de tarjeta</label>
              <input
                type="text"
                inputMode="numeric"
                placeholder="1234 5678 9012 3456"
                value={cardNumber}
                onChange={(e) => {
                  const v = e.target.value.replace(/\D/g, '').slice(0, 19)
                  setCardNumber(v.replace(/(\d{4})(?=\d)/g, '$1 '))
                }}
                maxLength={23}
                className="input-card-number"
              />
            </div>
            <div className="form-row form-row-expiry">
              <label>Vencimiento (MM/AA)</label>
              <input
                type="text"
                inputMode="numeric"
                placeholder="MM/AA"
                value={expiry}
                onChange={(e) => {
                  let v = e.target.value.replace(/\D/g, '').slice(0, 4)
                  if (v.length >= 2) v = v.slice(0, 2) + '/' + v.slice(2)
                  setExpiry(v)
                }}
                maxLength={5}
                className="input-expiry"
              />
            </div>
          </div>
        </section>
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Procesando…' : 'Confirmar pedido'}
        </button>
      </form>
    </div>
  )
}
