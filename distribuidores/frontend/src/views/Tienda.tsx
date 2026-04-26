import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { buscarCatalogoUnificado, catalogLineKey, type CatalogPart } from '../api/repuestos'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useCurrency } from '../context/CurrencyContext'
import { useToast } from '../context/ToastContext'
import { LoadingModal } from '../components/LoadingModal'
import './Tienda.css'

const API_IMAGES = (import.meta.env.VITE_API_URL || 'http://localhost:5080').replace(/\/$/, '')

export function Tienda() {
  const [parts, setParts] = useState<CatalogPart[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const { isLoggedIn } = useAuth()
  const { add } = useCart()
  const { formatCatalog } = useCurrency()
  const toast = useToast()
  const navigate = useNavigate()

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    const asCatalog = (rows: CatalogPart[]): CatalogPart[] =>
      rows.map((p) => ({ ...p, source: p.source ?? 'local' }))

    buscarCatalogoUnificado(search.trim())
      .then((r) => { if (!cancelled) setParts(asCatalog(Array.isArray(r) ? r : [])) })
      .catch(() => { if (!cancelled) setParts([]) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [search])

  function handleAdd(part: CatalogPart) {
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
        <p className="page-subtitle">
          Catálogo local y fábricas conectadas. Los precios de referencia están en USD; elige la divisa en la barra
          lateral para ver el equivalente. Usa el buscador para acotar por nombre o compatibilidad; si está vacío se muestra todo lo
          activo.
        </p>
      </header>
      <div className="search-bar">
        <input
          type="text"
          placeholder="Filtrar por nombre, marca/modelo/año (opcional)…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      {loading && <div className="loading">Cargando...</div>}
      {!loading && (
        <div className="products-grid">
          {parts.filter((p) => p.active !== 0).map((part) => (
            <div key={catalogLineKey(part)} className="product-card">
              <Link
                to={
                  part.source === 'fabrica' && part.proveedorId != null
                    ? `/producto/fabrica/${part.proveedorId}/${part.partId}`
                    : `/producto/${part.partId}`
                }
                className="product-image"
              >
                {part.hasImage ? (
                  <img
                    src={
                      part.source === 'fabrica' && part.fabricaBaseUrl
                        ? `${part.fabricaBaseUrl.replace(/\/$/, '')}/api/images/part/${part.partId}`
                        : `${API_IMAGES}/api/images/part/${part.partId}`
                    }
                    alt={part.title}
                  />
                ) : (
                  <span className="no-image">📦</span>
                )}
              </Link>
              <div className="product-info">
                {part.source === 'fabrica' && part.proveedorId != null ? (
                  <Link
                    to={`/producto/fabrica/${part.proveedorId}/${part.partId}`}
                    className="product-title-link"
                  >
                    <h3>{part.title}</h3>
                  </Link>
                ) : (
                  <Link to={`/producto/${part.partId}`} className="product-title-link"><h3>{part.title}</h3></Link>
                )}
                {part.source === 'fabrica' && part.proveedorNombre && (
                  <p className="part-number">Fábrica: {part.proveedorNombre}</p>
                )}
                <p className="part-number">{part.partNumber}</p>
                {part.compatibilityTags && <p className="part-compat">Compatibilidad: {part.compatibilityTags}</p>}
                <p className="price">{formatCatalog(Number(part.price))}</p>
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
