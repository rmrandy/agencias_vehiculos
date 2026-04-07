import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listProveedores, deleteProveedor, type ProveedorDto } from '../api/proveedores'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { LoadingModal } from '../components/LoadingModal'
import './ProveedoresAdmin.css'

export function ProveedoresAdmin() {
  const { user, isLoggedIn } = useAuth()
  const [rows, setRows] = useState<ProveedorDto[]>([])
  const [loading, setLoading] = useState(true)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const toast = useToast()

  const isAdmin = isLoggedIn && user?.roles?.includes('ADMIN')

  useEffect(() => {
    if (!isAdmin) {
      setLoading(false)
      return
    }
    listProveedores(true)
      .then(setRows)
      .catch(() => {
        setRows([])
        toast.error('No se pudo cargar la lista de fábricas')
      })
      .finally(() => setLoading(false))
  }, [isAdmin])

  async function handleDelete(p: ProveedorDto) {
    if (!window.confirm(`¿Eliminar el proveedor / fábrica «${p.nombre}»?`)) return
    setDeletingId(p.proveedorId)
    try {
      await deleteProveedor(p.proveedorId)
      setRows((prev) => prev.filter((x) => x.proveedorId !== p.proveedorId))
      toast.success('Eliminado')
    } catch {
      toast.error('No se pudo eliminar')
    } finally {
      setDeletingId(null)
    }
  }

  if (!isLoggedIn || !user) {
    return (
      <div className="proveedores-admin">
        <p>Debes iniciar sesión.</p>
        <Link to="/login">Iniciar sesión</Link>
      </div>
    )
  }

  if (!isAdmin) {
    return (
      <div className="proveedores-admin">
        <p>Solo los administradores pueden gestionar fábricas conectadas.</p>
        <Link to="/">Volver al inicio</Link>
      </div>
    )
  }

  return (
    <div className="proveedores-admin">
      <LoadingModal open={loading} message="Cargando fábricas..." />
      <header className="page-header">
        <div>
          <h1>Fábricas conectadas</h1>
          <p className="page-subtitle">
            Proveedores con API: URL base del backend Java y usuario de pedidos en esa fábrica. Aparecen en el
            catálogo al buscar.
          </p>
        </div>
        <Link to="/fabricas/nuevo" className="btn btn-primary">
          Nueva fábrica
        </Link>
      </header>
      {loading && <p>Cargando...</p>}
      {!loading && (
        <div className="proveedores-table-wrap">
          <table className="proveedores-table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>URL API</th>
                <th>User ID fábrica</th>
                <th>Activo</th>
                <th>Notas</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr>
                  <td colSpan={6} className="proveedores-empty">
                    No hay fábricas dadas de alta. Pulsa «Nueva fábrica» para añadir una (ej.{' '}
                    <code>http://localhost:9090</code>).
                  </td>
                </tr>
              )}
              {rows.map((p) => (
                <tr key={p.proveedorId} className={!p.activo ? 'inactive' : ''}>
                  <td>
                    <strong>{p.nombre}</strong>
                    {p.esInternacional && <span className="badge-intl">Internacional</span>}
                  </td>
                  <td className="cell-url">{p.apiBaseUrl || '—'}</td>
                  <td>{p.fabricaEnterpriseUserId ?? '—'}</td>
                  <td>{p.activo ? 'Sí' : 'No'}</td>
                  <td className="cell-muted">
                    {[p.email, p.telefono].filter(Boolean).join(' · ') || '—'}
                  </td>
                  <td>
                    <Link to={`/fabricas/editar/${p.proveedorId}`} className="btn btn-sm">
                      Editar
                    </Link>
                    <button
                      type="button"
                      className="btn btn-sm btn-danger"
                      disabled={deletingId === p.proveedorId}
                      onClick={() => handleDelete(p)}
                    >
                      {deletingId === p.proveedorId ? '…' : 'Eliminar'}
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
