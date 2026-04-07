import { useEffect, useState } from 'react'
import { useNavigate, useParams, Link } from 'react-router-dom'
import { getProveedor, createProveedor, updateProveedor, type SaveProveedorBody } from '../api/proveedores'
import { useAuth } from '../context/AuthContext'
import { LoadingModal } from '../components/LoadingModal'
import { useToast } from '../context/ToastContext'
import './ProveedorForm.css'

function parseOptionalLong(s: string): number | undefined {
  const t = s.trim()
  if (!t) return undefined
  const n = parseInt(t, 10)
  return Number.isFinite(n) ? n : undefined
}

function parseOptionalDecimal(s: string): number | undefined {
  const t = s.trim().replace(',', '.')
  if (!t) return undefined
  const n = parseFloat(t)
  return Number.isFinite(n) ? n : undefined
}

export function ProveedorForm() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const toast = useToast()
  const { user, isLoggedIn } = useAuth()
  const isEdit = !!id
  const proveedorId = id ? parseInt(id, 10) : 0

  const isAdmin = isLoggedIn && user?.roles?.includes('ADMIN')

  const [loading, setLoading] = useState(!!id)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const [nombre, setNombre] = useState('')
  const [apiBaseUrl, setApiBaseUrl] = useState('')
  const [fabricaEnterpriseUserId, setFabricaEnterpriseUserId] = useState('')
  const [contacto, setContacto] = useState('')
  const [email, setEmail] = useState('')
  const [telefono, setTelefono] = useState('')
  const [activo, setActivo] = useState(true)
  const [tipoCambio, setTipoCambio] = useState('')
  const [porcentajeGanancia, setPorcentajeGanancia] = useState('')
  const [costoEnvioPorLibra, setCostoEnvioPorLibra] = useState('')

  useEffect(() => {
    if (!isEdit || !Number.isFinite(proveedorId)) return
    getProveedor(proveedorId)
      .then((p) => {
        setNombre(p.nombre)
        setApiBaseUrl(p.apiBaseUrl ?? '')
        setFabricaEnterpriseUserId(
          p.fabricaEnterpriseUserId != null ? String(p.fabricaEnterpriseUserId) : ''
        )
        setContacto(p.contacto ?? '')
        setEmail(p.email ?? '')
        setTelefono(p.telefono ?? '')
        setActivo(p.activo)
        setTipoCambio(p.tipoCambioAQuetzales != null ? String(p.tipoCambioAQuetzales) : '')
        setPorcentajeGanancia(p.porcentajeGanancia != null ? String(p.porcentajeGanancia) : '')
        setCostoEnvioPorLibra(p.costoEnvioPorLibra != null ? String(p.costoEnvioPorLibra) : '')
      })
      .catch(() => setError('No se pudo cargar el proveedor'))
      .finally(() => setLoading(false))
  }, [isEdit, proveedorId])

  function buildBody(): SaveProveedorBody | null {
    setError('')
    if (!nombre.trim()) {
      setError('El nombre es obligatorio')
      return null
    }

    const tc = parseOptionalDecimal(tipoCambio)
    const pg = parseOptionalDecimal(porcentajeGanancia)
    const ce = parseOptionalDecimal(costoEnvioPorLibra)
    const anyIntl = tc != null || pg != null || ce != null
    if (anyIntl && (tc == null || tc <= 0)) {
      setError('Si usas datos internacionales, tipo de cambio a quetzales debe ser mayor que 0')
      return null
    }

    const body: SaveProveedorBody = {
      nombre: nombre.trim(),
      apiBaseUrl: apiBaseUrl.trim() || null,
      fabricaEnterpriseUserId: parseOptionalLong(fabricaEnterpriseUserId),
      contacto: contacto.trim() || null,
      email: email.trim() || null,
      telefono: telefono.trim() || null,
      activo,
      tipoCambioAQuetzales: tc,
      porcentajeGanancia: pg,
      costoEnvioPorLibra: ce,
    }
    return body
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const body = buildBody()
    if (!body) return
    setSaving(true)
    try {
      if (isEdit) {
        await updateProveedor(proveedorId, body)
        toast.success('Fábrica actualizada')
      } else {
        await createProveedor(body)
        toast.success('Fábrica creada')
      }
      navigate('/fabricas')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al guardar')
    } finally {
      setSaving(false)
    }
  }

  if (!isLoggedIn || !user) {
    return (
      <div className="proveedor-form-page">
        <p>Debes iniciar sesión.</p>
        <Link to="/login">Iniciar sesión</Link>
      </div>
    )
  }

  if (!isAdmin) {
    return (
      <div className="proveedor-form-page">
        <p>Solo administradores pueden editar fábricas conectadas.</p>
        <Link to="/">Inicio</Link>
      </div>
    )
  }

  return (
    <div className="proveedor-form-page">
      <LoadingModal open={loading} message="Cargando..." />
      <header className="proveedor-form-header">
        <h1>{isEdit ? 'Editar fábrica' : 'Nueva fábrica'}</h1>
        <Link to="/fabricas" className="btn btn-sm">
          Volver al listado
        </Link>
      </header>

      <form className="proveedor-form-card" onSubmit={handleSubmit}>
        {error && <div className="proveedor-form-error">{error}</div>}

        <fieldset>
          <legend>Conexión al API de la fábrica</legend>
          <label>
            Nombre visible <span className="req">*</span>
            <input
              type="text"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              placeholder="Ej. Planta Norte"
              required
            />
          </label>
          <label>
            URL base del API (Java)
            <input
              type="url"
              value={apiBaseUrl}
              onChange={(e) => setApiBaseUrl(e.target.value)}
              placeholder="http://localhost:9090"
            />
            <span className="hint">Sin barra final. Desde aquí se llama /api/repuestos y /api/pedidos.</span>
          </label>
          <label>
            User ID en la fábrica (pedidos)
            <input
              type="text"
              inputMode="numeric"
              value={fabricaEnterpriseUserId}
              onChange={(e) => setFabricaEnterpriseUserId(e.target.value.replace(/\D/g, ''))}
              placeholder="Ej. 1"
            />
            <span className="hint">Debe existir en APP_USER de esa base Oracle; se envía como userId al crear pedidos.</span>
          </label>
          <label className="checkbox-row">
            <input type="checkbox" checked={activo} onChange={(e) => setActivo(e.target.checked)} />
            Activo (aparece en búsqueda unificada)
          </label>
        </fieldset>

        <fieldset>
          <legend>Contacto (opcional)</legend>
          <label>
            Contacto
            <input type="text" value={contacto} onChange={(e) => setContacto(e.target.value)} />
          </label>
          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <label>
            Teléfono
            <input type="text" value={telefono} onChange={(e) => setTelefono(e.target.value)} />
          </label>
        </fieldset>

        <fieldset>
          <legend>Internacional (opcional, DOC2)</legend>
          <p className="fieldset-hint">
            Si rellenas cualquiera de estos tres, debes indicar tipo de cambio a quetzales (&gt; 0).
          </p>
          <label>
            Tipo de cambio (GTQ por 1 USD u otra moneda)
            <input type="text" value={tipoCambio} onChange={(e) => setTipoCambio(e.target.value)} placeholder="Ej. 7.85" />
          </label>
          <label>
            Porcentaje ganancia (%)
            <input type="text" value={porcentajeGanancia} onChange={(e) => setPorcentajeGanancia(e.target.value)} />
          </label>
          <label>
            Costo envío por libra
            <input type="text" value={costoEnvioPorLibra} onChange={(e) => setCostoEnvioPorLibra(e.target.value)} />
          </label>
        </fieldset>

        <div className="proveedor-form-actions">
          <button type="submit" className="btn btn-primary" disabled={saving || loading}>
            {saving ? 'Guardando…' : isEdit ? 'Guardar cambios' : 'Crear fábrica'}
          </button>
          <Link to="/fabricas" className="btn">
            Cancelar
          </Link>
        </div>
      </form>
    </div>
  )
}
