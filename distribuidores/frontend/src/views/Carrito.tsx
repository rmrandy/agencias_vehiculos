import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { getPartImageUrl, catalogLineKey } from '../api/repuestos'
import './Carrito.css'

export function Carrito() {
  const { items, removeLine, setQtyLine } = useCart()
  const { isLoggedIn } = useAuth()
  const navigate = useNavigate()

  if (!isLoggedIn) {
    return (
      <div className="carrito-page">
        <h1>Carrito</h1>
        <p>Inicia sesión para ver y gestionar tu carrito.</p>
        <button type="button" className="btn btn-primary" onClick={() => navigate('/login')}>
          Iniciar sesión
        </button>
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div className="carrito-page carrito-empty">
        <div className="carrito-empty-card">
          <div className="carrito-empty-icon" aria-hidden>🛒</div>
          <h1>Carrito vacío</h1>
          <p className="carrito-empty-text">Aún no tienes repuestos en el carrito.</p>
          <p className="carrito-empty-hint">Explora el catálogo y añade los productos que necesites.</p>
          <Link to="/tienda" className="btn btn-primary btn-lg carrito-empty-cta">
            Ir al catálogo
          </Link>
        </div>
      </div>
    )
  }

  const subtotal = items.reduce((s, x) => s + x.part.price * x.qty, 0)

  return (
    <div className="carrito-page">
      <h1>Carrito</h1>
      <p className="carrito-count">{items.length} {items.length === 1 ? 'producto' : 'productos'}</p>
      <ul className="carrito-list">
        {items.map(({ part, qty }) => {
          const lineKey = catalogLineKey(part)
          const lineTotal = part.price * qty
          const imageUrl = part.hasImage
            ? getPartImageUrl(part.partId, 0, part.source === 'fabrica' ? part.fabricaBaseUrl : undefined)
            : null
          const detalleTo = part.source === 'fabrica' ? '#' : `/producto/${part.partId}`
          return (
            <li key={lineKey} className="carrito-item">
              <Link
                to={detalleTo}
                className="carrito-item-image"
                onClick={(e) => {
                  if (part.source === 'fabrica') e.preventDefault()
                }}
              >
                {imageUrl ? (
                  <img src={imageUrl} alt={part.title} />
                ) : (
                  <span className="carrito-item-no-image">📦</span>
                )}
              </Link>
              <div className="carrito-item-info">
                {part.source === 'fabrica' ? (
                  <span className="carrito-item-title">{part.title}</span>
                ) : (
                  <Link to={detalleTo} className="carrito-item-title">
                    {part.title}
                  </Link>
                )}
                {part.source === 'fabrica' && part.proveedorNombre && (
                  <p className="carrito-item-code">Fábrica: {part.proveedorNombre}</p>
                )}
                {part.partNumber && (
                  <p className="carrito-item-code">Código: {part.partNumber}</p>
                )}
                {part.description && (
                  <p className="carrito-item-desc">{part.description.slice(0, 80)}{part.description.length > 80 ? '…' : ''}</p>
                )}
                <p className="carrito-item-unit">Precio unitario: ${Number(part.price).toFixed(2)}</p>
              </div>
              <div className="carrito-item-qty">
                <label>Cantidad</label>
                <input
                  type="number"
                  min={1}
                  value={qty}
                  onChange={(e) => setQtyLine(lineKey, Math.max(1, parseInt(e.target.value, 10) || 1))}
                />
              </div>
              <div className="carrito-item-total">
                <span className="carrito-item-line-total">${lineTotal.toFixed(2)}</span>
                <button type="button" className="btn btn-sm btn-danger" onClick={() => removeLine(lineKey)}>
                  Quitar
                </button>
              </div>
            </li>
          )
        })}
      </ul>
      <div className="carrito-footer">
        <div className="carrito-subtotal">
          <span>Subtotal:</span>
          <strong>${subtotal.toFixed(2)}</strong>
        </div>
        <button type="button" className="btn btn-primary btn-lg" onClick={() => navigate('/checkout')}>
          Ir a pagar
        </button>
      </div>
    </div>
  )
}
