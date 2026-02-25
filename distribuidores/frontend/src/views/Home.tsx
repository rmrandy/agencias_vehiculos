import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getHealth, type HealthResponse } from '../api/health'
import './Home.css'

export function Home() {
  const { isLoggedIn } = useAuth()
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getHealth()
      .then(setHealth)
      .catch((e) => setError(e instanceof Error ? e.message : 'No se pudo conectar al backend'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1>Distribuidora</h1>
        <p className="dashboard-subtitle">Catálogo de repuestos y pedidos — usuarios empresariales</p>
      </header>

      <div className="dashboard-grid">
        <section className="card card-welcome">
          <h2 className="card-title">Bienvenido</h2>
          <p className="card-desc">
            Sistema para distribuidores. Busca repuestos, agrega al carrito y realiza pedidos con tu cuenta empresarial.
          </p>
          <div className="quick-links">
            <Link to="/tienda" className="quick-link">Ir al catálogo</Link>
            {!isLoggedIn && <Link to="/login" className="quick-link">Iniciar sesión</Link>}
          </div>
        </section>

        <section className="card card-health">
          <h2 className="card-title">Estado del backend</h2>
          {loading && (
            <div className="status status-loading">
              <span className="status-dot" />
              Comprobando conexión...
            </div>
          )}
          {error && (
            <div className="status status-error">
              <span className="status-dot" />
              {error}
            </div>
          )}
          {!loading && !error && health && (
            <div className="status status-ok">
              <span className="status-badge">OK</span>
              <span>API en línea — {health.status}</span>
            </div>
          )}
        </section>

        <section className="card card-placeholder">
          <h2 className="card-title">Accesos rápidos</h2>
          <div className="quick-links">
            <Link to="/tienda" className="quick-link">Catálogo</Link>
            <Link to="/carrito" className="quick-link">Carrito</Link>
            <Link to="/pedidos" className="quick-link">Mis pedidos</Link>
          </div>
        </section>
      </div>
    </div>
  )
}
