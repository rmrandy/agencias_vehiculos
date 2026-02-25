import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listRepuestos, deleteRepuesto, type Part } from '../api/repuestos'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { LoadingModal } from '../components/LoadingModal'
import './ProductosAdmin.css'

const API_IMAGES = (import.meta.env.VITE_API_URL || 'http://localhost:5080').replace(/\/$/, '')

export function ProductosAdmin() {
  const { user, isLoggedIn } = useAuth()
  const [parts, setParts] = useState<Part[]>([])
  const [loading, setLoading] = useState(true)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const toast = useToast()

  useEffect(() => {
    listRepuestos({ includeInactive: true })
      .then(setParts)
      .catch(() => setParts([]))
      .finally(() => setLoading(false))
  }, [])

  async function handleDelete(part: Part) {
    if (!window.confirm(`¿Eliminar "${part.title}" (${part.partNumber})?`)) return
    setDeletingId(part.partId)
    try {
      await deleteRepuesto(part.partId)
      setParts((prev) => prev.filter((p) => p.partId !== part.partId))
      toast.success('Producto eliminado')
    } catch {
      toast.error('No se pudo eliminar')
    } finally {
      setDeletingId(null)
    }
  }

  if (!isLoggedIn || !user) {
    return (
      <div className="productos-admin">
        <p>Debes iniciar sesión para gestionar productos.</p>
        <Link to="/login">Iniciar sesión</Link>
      </div>
    )
  }

  return (
    <div className="productos-admin">
      <LoadingModal open={loading} message="Cargando productos..." />
      <header className="page-header">
        <h1>Gestión de productos</h1>
        <p className="page-subtitle">CRUD de repuestos (catálogo local)</p>
        <Link to="/productos/nuevo" className="btn btn-primary">
          Nuevo producto
        </Link>
      </header>
      {loading && <p>Cargando...</p>}
      {!loading && (
        <div className="productos-table-wrap">
          <table className="productos-table">
            <thead>
              <tr>
                <th>Imagen</th>
                <th>Código</th>
                <th>Título</th>
                <th>Precio</th>
                <th>Stock</th>
                <th>Activo</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {parts.map((p) => (
                <tr key={p.partId} className={p.active === 0 ? 'inactive' : ''}>
                  <td>
                    {p.hasImage ? (
                      <img
                        src={`${API_IMAGES}/api/images/part/${p.partId}`}
                        alt=""
                        className="thumb"
                      />
                    ) : (
                      <span className="no-thumb">—</span>
                    )}
                  </td>
                  <td>{p.partNumber}</td>
                  <td>{p.title}</td>
                  <td>${Number(p.price).toFixed(2)}</td>
                  <td>{p.stockQuantity ?? p.availableQuantity ?? 0}</td>
                  <td>{p.active !== 0 ? 'Sí' : 'No'}</td>
                  <td>
                    <Link to={`/productos/editar/${p.partId}`} className="btn btn-sm">
                      Editar
                    </Link>
                    <button
                      type="button"
                      className="btn btn-sm btn-danger"
                      disabled={deletingId === p.partId}
                      onClick={() => handleDelete(p)}
                    >
                      {deletingId === p.partId ? '…' : 'Eliminar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
