import { useEffect, useMemo, useState } from 'react'
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
  const [sourceFilter, setSourceFilter] = useState<'all' | 'local' | 'fabrica'>('all')
  const [selectedProveedor, setSelectedProveedor] = useState<string>('all')
  const [onlyInStock, setOnlyInStock] = useState(true)
  const [onlyWithImage, setOnlyWithImage] = useState(false)
  const [onlyBestPriceByTitle, setOnlyBestPriceByTitle] = useState(true)
  const [sortBy, setSortBy] = useState<'priceAsc' | 'priceDesc' | 'titleAsc' | 'stockDesc'>('priceAsc')
  const [page, setPage] = useState(1)
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

  useEffect(() => {
    setPage(1)
  }, [search, sourceFilter, selectedProveedor, onlyInStock, onlyWithImage, onlyBestPriceByTitle, sortBy])

  function handleAdd(part: CatalogPart) {
    if (!isLoggedIn) {
      navigate('/login')
      return
    }
    add(part, 1)
    toast.success('Agregado al carrito')
  }

  const proveedores = useMemo(() => {
    const map = new Map<number, string>()
    for (const p of parts) {
      if (p.source === 'fabrica' && p.proveedorId != null) {
        map.set(p.proveedorId, p.proveedorNombre || `Proveedor ${p.proveedorId}`)
      }
    }
    return Array.from(map.entries())
      .map(([id, name]) => ({ id, name }))
      .sort((a, b) => a.name.localeCompare(b.name, 'es'))
  }, [parts])

  const filtered = useMemo(() => {
    let rows = parts.filter((p) => p.active !== 0)

    if (sourceFilter !== 'all') rows = rows.filter((p) => (p.source ?? 'local') === sourceFilter)
    if (selectedProveedor !== 'all') {
      const provId = Number(selectedProveedor)
      rows = rows.filter((p) => p.proveedorId === provId)
    }
    if (onlyInStock) rows = rows.filter((p) => p.inStock !== false)
    if (onlyWithImage) rows = rows.filter((p) => p.hasImage === true)

    if (onlyBestPriceByTitle) {
      const byTitle = new Map<string, CatalogPart>()
      for (const row of rows) {
        const key = (row.title || '').trim().toLowerCase()
        const prev = byTitle.get(key)
        if (!prev || Number(row.price) < Number(prev.price)) {
          byTitle.set(key, row)
        }
      }
      rows = Array.from(byTitle.values())
    }

    rows.sort((a, b) => {
      if (sortBy === 'priceAsc') return Number(a.price) - Number(b.price)
      if (sortBy === 'priceDesc') return Number(b.price) - Number(a.price)
      if (sortBy === 'stockDesc') return Number(b.availableQuantity ?? 0) - Number(a.availableQuantity ?? 0)
      return (a.title || '').localeCompare(b.title || '', 'es')
    })
    return rows
  }, [parts, sourceFilter, selectedProveedor, onlyInStock, onlyWithImage, onlyBestPriceByTitle, sortBy])

  const pageSize = 24
  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize))
  const pageSafe = Math.min(page, totalPages)
  const pageRows = filtered.slice((pageSafe - 1) * pageSize, pageSafe * pageSize)

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
      <div className="catalog-filters">
        <label>
          Fuente:
          <select value={sourceFilter} onChange={(e) => setSourceFilter(e.target.value as 'all' | 'local' | 'fabrica')}>
            <option value="all">Todas</option>
            <option value="local">Solo distribuidora</option>
            <option value="fabrica">Solo fábricas</option>
          </select>
        </label>
        <label>
          Fábrica:
          <select value={selectedProveedor} onChange={(e) => setSelectedProveedor(e.target.value)}>
            <option value="all">Todas</option>
            {proveedores.map((p) => (
              <option key={p.id} value={String(p.id)}>
                {p.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Orden:
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value as 'priceAsc' | 'priceDesc' | 'titleAsc' | 'stockDesc')}>
            <option value="priceAsc">Precio: menor a mayor</option>
            <option value="priceDesc">Precio: mayor a menor</option>
            <option value="titleAsc">Nombre A-Z</option>
            <option value="stockDesc">Mayor disponibilidad</option>
          </select>
        </label>
        <label className="check">
          <input type="checkbox" checked={onlyInStock} onChange={(e) => setOnlyInStock(e.target.checked)} />
          Solo con stock
        </label>
        <label className="check">
          <input type="checkbox" checked={onlyWithImage} onChange={(e) => setOnlyWithImage(e.target.checked)} />
          Solo con imagen
        </label>
        <label className="check">
          <input
            type="checkbox"
            checked={onlyBestPriceByTitle}
            onChange={(e) => setOnlyBestPriceByTitle(e.target.checked)}
          />
          Mejor precio por nombre
        </label>
      </div>
      <p className="catalog-results-count">
        Mostrando {pageRows.length} de {filtered.length} resultados
      </p>
      {loading && <div className="loading">Cargando...</div>}
      {!loading && (
        <div className="products-grid">
          {pageRows.map((part) => (
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
      {!loading && filtered.length === 0 && <p className="no-results">No hay repuestos con esos filtros</p>}
      {!loading && totalPages > 1 && (
        <div className="catalog-pagination">
          <button className="btn btn-secondary" disabled={pageSafe <= 1} onClick={() => setPage((p) => Math.max(1, p - 1))}>
            Anterior
          </button>
          <span>
            Página {pageSafe} de {totalPages}
          </span>
          <button
            className="btn btn-secondary"
            disabled={pageSafe >= totalPages}
            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
          >
            Siguiente
          </button>
        </div>
      )}
    </div>
  )
}
