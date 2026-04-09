import { useState, useEffect, useRef, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useCurrency } from '../context/CurrencyContext'
import { catalogLineKey } from '../api/repuestos'
import { createPedido } from '../api/pedidos'
import {
  getAranceles,
  getPaisesLatam,
  getTarifaEnvioPorLibra,
  type ArancelPaisRow,
  type PaisLatamOption,
} from '../api/aranceles'
import { LoadingModal } from '../components/LoadingModal'
import './Checkout.css'

export function Checkout() {
  const { user, isLoggedIn } = useAuth()
  const { items, clear } = useCart()
  const { formatCatalog, selectedCode } = useCurrency()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [cardNumber, setCardNumber] = useState('')
  const [expiry, setExpiry] = useState('')
  const [shippingCountryCode, setShippingCountryCode] = useState('')
  const [paisesLatam, setPaisesLatam] = useState<PaisLatamOption[]>([])
  const [aranceles, setAranceles] = useState<ArancelPaisRow[]>([])
  const [usdPerLb, setUsdPerLb] = useState(0)
  /** Evita que, tras vaciar el carrito al confirmar, el efecto redirija a /carrito en lugar de /gracias. */
  const skipEmptyCartRedirectRef = useRef(false)

  useEffect(() => {
    let cancelled = false
    getTarifaEnvioPorLibra()
      .then((r) => {
        if (!cancelled) setUsdPerLb(Number(r.usdPerLb) || 0)
      })
      .catch(() => {
        if (!cancelled) setUsdPerLb(0)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!isLoggedIn || !user) {
      navigate('/login')
      return
    }
    if (items.length === 0 && !skipEmptyCartRedirectRef.current) navigate('/carrito')
  }, [isLoggedIn, user, items.length, navigate])

  const hasFabricaLines = useMemo(
    () => items.some((x) => x.part.source === 'fabrica'),
    [items]
  )

  useEffect(() => {
    if (!hasFabricaLines) return
    let cancelled = false
    Promise.all([getPaisesLatam(), getAranceles()])
      .then(([p, a]) => {
        if (!cancelled) {
          setPaisesLatam(p)
          setAranceles(a)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setPaisesLatam([])
          setAranceles([])
        }
      })
    return () => {
      cancelled = true
    }
  }, [hasFabricaLines])

  const importSubtotal = useMemo(
    () =>
      items.reduce((s, x) => {
        if (x.part.source === 'fabrica') return s + x.part.price * x.qty
        return s
      }, 0),
    [items]
  )

  const tariffPercent =
    shippingCountryCode && aranceles.length
      ? aranceles.find((a) => a.countryCode === shippingCountryCode)?.tariffPercent ?? 0
      : 0
  const tariffEstimate = Math.round(importSubtotal * (tariffPercent / 100) * 100) / 100

  const orderItems = items.map((x) => {
    const p = x.part
    if (p.source === 'fabrica' && p.proveedorId != null) {
      return {
        source: 'fabrica' as const,
        proveedorId: p.proveedorId,
        fabricaPartId: p.partId,
        qty: x.qty,
        unitPrice: p.price,
        ...(p.weightLb != null ? { weightLb: p.weightLb } : {}),
        title: p.title,
        partNumber: p.partNumber,
      }
    }
    return { source: 'local' as const, partId: p.partId, qty: x.qty }
  })
  const subtotal = items.reduce((s, x) => s + x.part.price * x.qty, 0)
  const totalWeightLb = useMemo(
    () =>
      items.reduce((s, x) => {
        const w = x.part.weightLb != null ? Number(x.part.weightLb) : 0
        if (w > 0) return s + w * x.qty
        return s
      }, 0),
    [items]
  )
  const shippingEstimate =
    usdPerLb > 0 && totalWeightLb > 0 ? Math.round(totalWeightLb * usdPerLb * 100) / 100 : 0
  const grandTotal = subtotal + shippingEstimate + (hasFabricaLines ? tariffEstimate : 0)
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
      if (hasFabricaLines) {
        if (!shippingCountryCode.trim()) {
          setError('Selecciona el país de destino del envío (LATAM) para pedidos con repuestos de fábrica.')
          setLoading(false)
          return
        }
      }
      const exp = parseExpiry()
      const payment = hasPayment && exp
        ? {
            cardNumber: digitsOnly,
            expiryMonth: exp.month,
            expiryYear: exp.year,
          }
        : undefined
      const order = await createPedido(
        user.userId,
        orderItems,
        payment,
        hasFabricaLines ? shippingCountryCode.trim().toUpperCase() : null,
        selectedCode
      )
      skipEmptyCartRedirectRef.current = true
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
      <p className="checkout-currency-hint">
        Importes mostrados y cobro en <strong>{selectedCode}</strong> (tipo de cambio configurado por la distribuidora).
      </p>
      {error && <div className="checkout-error">{error}</div>}
      <div className="checkout-summary">
        <strong>Resumen</strong>
        <ul>
          {items.map(({ part, qty }) => (
            <li key={catalogLineKey(part)}>
              {part.source === 'fabrica' && (
                <span className="checkout-line-meta">Pedido en fábrica · {part.proveedorNombre ?? 'Proveedor'}</span>
              )}
              <span>
                {part.title} × {qty} = {formatCatalog(part.price * qty)}
              </span>
            </li>
          ))}
        </ul>
        {hasFabricaLines && (
          <div className="checkout-shipping-country">
            <label htmlFor="shipping-country">País de destino del envío (LATAM)</label>
            <select
              id="shipping-country"
              className="checkout-select"
              value={shippingCountryCode}
              onChange={(e) => setShippingCountryCode(e.target.value)}
              required={hasFabricaLines}
            >
              <option value="">Seleccionar…</option>
              {paisesLatam.map((p) => (
                <option key={p.countryCode} value={p.countryCode}>
                  {p.countryName} ({p.countryCode})
                </option>
              ))}
            </select>
            <p className="checkout-tariff-hint">
              Arancel estimado sobre importado (fábrica): {tariffPercent.toFixed(2)}% →{' '}
              {formatCatalog(tariffEstimate)}
            </p>
          </div>
        )}
        {(usdPerLb > 0 || totalWeightLb > 0) && (
          <p className="checkout-shipping-estimate">
            Peso total (líneas con peso): {totalWeightLb.toFixed(2)} lb
            {usdPerLb > 0 && totalWeightLb > 0 && (
              <>
                {' '}
                · Envío estimado ({totalWeightLb.toFixed(2)} lb × {formatCatalog(usdPerLb)}/lb):{' '}
                {formatCatalog(shippingEstimate)}
              </>
            )}
            {usdPerLb > 0 && totalWeightLb === 0 && (
              <span className="checkout-shipping-warn"> · Añade peso a los productos para calcular envío.</span>
            )}
          </p>
        )}
        <p className="total">Total: {formatCatalog(grandTotal)}</p>
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
