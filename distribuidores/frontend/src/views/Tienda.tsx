import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { listRepuestos, buscarRepuestos, type Part } from '../api/repuestos'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useToast } from '../context/ToastContext'
import { LoadingModal } from '../components/LoadingModal'
import './Tienda.css'

const API_IMAGES = (import.meta.env.VITE_API_URL || 'http://localhost:5080').replace(/\/$/, '')

export function Tienda() {
  const [parts, setParts] = useState<Part[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const { isLoggedIn } = useAuth()
  const { add } = useCart()
  const toast = useToast()
  const navigate = useNavigate()

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    if (search.trim()) {
      buscarRepuestos({ nombre: search.trim() })
        .then((r) => { if (!cancelled) setParts(r) })
        .catch(() => { if (!cancelled) setParts([]) })
        .finally(() => { if (!cancelled) setLoading(false) })
    } else {
      listRepuestos()
        .then((r) => { if (!cancelled) setParts(Array.isArray(r) ? r : []) })
        .catch(() => { if (!cancelled) setParts([]) })
        .finally(() => { if (!cancelled) setLoading(false) })
    }
    return () => { cancelled = true }
  }, [search])

  function handleAdd(part: Part) {
    if (!isLoggedIn) {
      navigate('/login')
      return
    }
    add(part, 1)
    toast.success('Agregado al carrito')
  }

  return (
    <div className="tienda-page">
      <LoadingModal open={loading} message="Cargando catálogo..." />
      <header className="page-header">
        <h1>Catálogo de repuestos</h1>
        <p className="page-subtitle">Catálogo local — busca y compra repuestos</p>
      </header>
      <div className="search-bar">
        <input
          type="text"
          placeholder="Buscar por nombre..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      {loading && <div className="loading">Cargando...</div>}
      {!loading && (
        <div className="products-grid">
          {parts.filter((p) => p.active !== 0).map((part) => (
            <div key={part.partId} className="product-card">
              <Link to={`/producto/${part.partId}`} className="product-image">
                {part.hasImage ? (
                  <img src={`${API_IMAGES}/api/images/part/${part.partId}`} alt={part.title} />
                ) : (
                  <span className="no-image">📦</span>
                )}
              </Link>
              <div className="product-info">
                <Link to={`/producto/${part.partId}`} className="product-title-link"><h3>{part.title}</h3></Link>
                <p className="part-number">{part.partNumber}</p>
                <p className="price">${Number(part.price).toFixed(2)}</p>
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled={!part.inStock}
                  onClick={() => handleAdd(part)}
                >
                  {part.inStock ? 'Agregar al carrito' : 'Sin stock'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
      {!loading && parts.length === 0 && <p className="no-results">No hay repuestos</p>}
    </div>
  )
}
