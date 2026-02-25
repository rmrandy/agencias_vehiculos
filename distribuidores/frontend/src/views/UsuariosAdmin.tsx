import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getUsuarios, updateUsuario, type UsuarioDto } from '../api/usuarios'
import { LoadingModal } from '../components/LoadingModal'
import { useToast } from '../context/ToastContext'
import './UsuariosAdmin.css'

const ROLES_AVAILABLE = ['USER', 'ADMIN', 'EMPLOYEE']

export function UsuariosAdmin() {
  const { user, isLoggedIn } = useAuth()
  const toast = useToast()
  const [usuarios, setUsuarios] = useState<UsuarioDto[]>([])
  const [loading, setLoading] = useState(true)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formStatus, setFormStatus] = useState('ACTIVE')
  const [formRoles, setFormRoles] = useState<string[]>([])

  const isAdmin = isLoggedIn && user?.roles?.includes('ADMIN')

  useEffect(() => {
    if (!isAdmin || !user) {
      setLoading(false)
      return
    }
    getUsuarios(user.userId)
      .then(setUsuarios)
      .catch(() => {
        setUsuarios([])
        toast.error('No tienes permiso o hubo un error')
      })
      .finally(() => setLoading(false))
  }, [isAdmin, user?.userId])

  function openEdit(u: UsuarioDto) {
    setEditingId(u.userId)
    setFormStatus(u.status)
    setFormRoles(u.roles ?? [])
  }

  async function handleSave() {
    if (!user || editingId == null) return
    try {
      await updateUsuario(editingId, {
        adminUserId: user.userId,
        status: formStatus,
        roleNames: formRoles,
      })
      toast.success('Usuario actualizado')
      setEditingId(null)
      getUsuarios(user.userId).then(setUsuarios)
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Error al guardar')
    }
  }

  function toggleRole(role: string) {
    setFormRoles((prev) =>
      prev.includes(role) ? prev.filter((r) => r !== role) : [...prev, role]
    )
  }

  if (!isLoggedIn || !user) {
    return (
      <div className="usuarios-admin-page">
        <p>Inicia sesión para acceder.</p>
      </div>
    )
  }

  if (!isAdmin) {
    return (
      <div className="usuarios-admin-page">
        <p>Solo los administradores pueden gestionar usuarios.</p>
      </div>
    )
  }

  return (
    <div className="usuarios-admin-page">
      <LoadingModal open={loading} message="Cargando usuarios..." />
      <header className="usuarios-admin-header">
        <h1>Gestión de usuarios</h1>
        <p className="page-subtitle">Lista de usuarios y roles (solo Admin).</p>
      </header>
      {!loading && usuarios.length === 0 && <p className="usuarios-admin-empty">No hay usuarios.</p>}
      {!loading && usuarios.length > 0 && (
        <div className="usuarios-admin-table-wrap">
          <table className="usuarios-admin-table">
            <thead>
              <tr>
                <th>Email</th>
                <th>Nombre</th>
                <th>Estado</th>
                <th>Roles</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u.userId}>
                  <td className="email-cell">{u.email}</td>
                  <td>{u.fullName ?? '—'}</td>
                  <td><span className={`status-pill status-${u.status?.toLowerCase() ?? 'active'}`}>{u.status ?? 'ACTIVE'}</span></td>
                  <td className="roles-cell">{(u.roles ?? []).join(', ') || '—'}</td>
                  <td>
                    <button type="button" className="btn btn-sm btn-primary" onClick={() => openEdit(u)}>
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {editingId != null && (
        <div className="modal-overlay" onClick={() => setEditingId(null)}>
          <div className="modal-content usuarios-admin-modal" onClick={(e) => e.stopPropagation()}>
            <h2>Editar usuario</h2>
            <div className="form-row">
              <label>Estado</label>
              <select value={formStatus} onChange={(e) => setFormStatus(e.target.value)}>
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
                <option value="SUSPENDED">SUSPENDED</option>
              </select>
            </div>
            <div className="form-row">
              <label>Roles</label>
              <div className="roles-checkboxes">
                {ROLES_AVAILABLE.map((role) => (
                  <label key={role} className="role-check">
                    <input type="checkbox" checked={formRoles.includes(role)} onChange={() => toggleRole(role)} />
                    <span>{role}</span>
                  </label>
                ))}
              </div>
            </div>
            <div className="modal-actions">
              <button type="button" className="btn btn-secondary" onClick={() => setEditingId(null)}>Cancelar</button>
              <button type="button" className="btn btn-primary" onClick={handleSave}>Guardar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
