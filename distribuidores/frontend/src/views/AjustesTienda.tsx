import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useStoreInfo } from '../context/StoreInfoContext'
import { useToast } from '../context/ToastContext'

export function AjustesTienda() {
  const { user, isLoggedIn } = useAuth()
  const { info, update, reset } = useStoreInfo()
  const toast = useToast()
  const isAdmin = isLoggedIn && user?.roles?.includes('ADMIN')

  const [name, setName] = useState(info.name)
  const [subtitle, setSubtitle] = useState(info.subtitle)
  const [generalInfo, setGeneralInfo] = useState(info.generalInfo)

  if (!isAdmin) {
    return (
      <div className="productos-admin">
        <h1>Ajustes de tienda</h1>
        <p>Solo administradores pueden editar el encabezado de la tienda.</p>
        <Link to="/">Volver al inicio</Link>
      </div>
    )
  }

  return (
    <div className="productos-admin">
      <header className="page-header">
        <h1>Ajustes del encabezado</h1>
        <p className="page-subtitle">Personaliza nombre de tienda e informacion general visible en la barra lateral.</p>
      </header>
      <form
        className="producto-form-fields"
        onSubmit={(e) => {
          e.preventDefault()
          update({ name, subtitle, generalInfo })
          toast.success('Encabezado actualizado')
        }}
      >
        <div className="form-group">
          <label>Nombre de tienda</label>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Distribuidora Centro" />
        </div>
        <div className="form-group">
          <label>Subtitulo</label>
          <input value={subtitle} onChange={(e) => setSubtitle(e.target.value)} placeholder="Agencias Vehiculos" />
        </div>
        <div className="form-group">
          <label>Informacion general</label>
          <input value={generalInfo} onChange={(e) => setGeneralInfo(e.target.value)} placeholder="Repuestos multimarca" />
        </div>
        <div className="form-actions">
          <button className="btn btn-primary" type="submit">
            Guardar
          </button>
          <button
            className="btn btn-secondary"
            type="button"
            onClick={() => {
              reset()
              setName('Distribuidora')
              setSubtitle('Agencias Vehiculos')
              setGeneralInfo('Repuestos multiorigen')
              toast.success('Valores restaurados')
            }}
          >
            Restaurar por defecto
          </button>
        </div>
      </form>
    </div>
  )
}
