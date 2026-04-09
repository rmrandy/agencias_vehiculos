import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useCurrency } from '../context/CurrencyContext'
import './Navbar.css'

export function Navbar() {
  const { user, isLoggedIn, logout } = useAuth()
  const { count } = useCart()
  const { monedas, selectedCode, setSelectedCode, loading: monedasLoading } = useCurrency()
  const location = useLocation()

  function isActive(path: string) {
    return location.pathname === path || (path !== '/' && location.pathname.startsWith(path))
  }

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <Link to="/" className="brand-link">
          <span className="brand-icon">📦</span>
          <span className="brand-text">Distribuidora</span>
          <span className="brand-sub">Agencias Vehículos</span>
        </Link>
      </div>
      <ul className="nav-links">
        <li>
          <Link to="/" className={`nav-link ${isActive('/') ? 'active' : ''}`}>
            <span className="nav-icon">◉</span> Inicio
          </Link>
        </li>
        <li>
          <Link to="/tienda" className={`nav-link ${isActive('/tienda') ? 'active' : ''}`}>
            <span className="nav-icon">🛍️</span> Catálogo
          </Link>
        </li>
        {isLoggedIn && (
          <>
            <li>
              <Link to="/carrito" className={`nav-link ${isActive('/carrito') ? 'active' : ''}`}>
                <span className="nav-icon">🛒</span> Carrito
                {count > 0 && <span className="cart-badge">{count}</span>}
              </Link>
            </li>
            <li>
              <Link to="/pedidos" className={`nav-link ${isActive('/pedidos') ? 'active' : ''}`}>
                <span className="nav-icon">📦</span> Mis pedidos
              </Link>
            </li>
            <li>
              <Link to="/productos" className={`nav-link ${isActive('/productos') ? 'active' : ''}`}>
                <span className="nav-icon">📋</span> Productos
              </Link>
            </li>
            {(user?.roles?.includes('ADMIN') || user?.roles?.includes('EMPLOYEE')) && (
              <li>
                <Link to="/pedidos-admin" className={`nav-link ${isActive('/pedidos-admin') ? 'active' : ''}`}>
                  <span className="nav-icon">📦</span> Gestión pedidos
                </Link>
              </li>
            )}
            {user?.roles?.includes('ADMIN') && (
              <>
                <li>
                  <Link to="/fabricas" className={`nav-link ${isActive('/fabricas') ? 'active' : ''}`}>
                    <span className="nav-icon">🏭</span> Fábricas
                  </Link>
                </li>
                <li>
                  <Link to="/usuarios" className={`nav-link ${isActive('/usuarios') ? 'active' : ''}`}>
                    <span className="nav-icon">👥</span> Usuarios
                  </Link>
                </li>
                <li>
                  <Link to="/aranceles" className={`nav-link ${isActive('/aranceles') ? 'active' : ''}`}>
                    <span className="nav-icon">🌎</span> Fiscal y divisas
                  </Link>
                </li>
              </>
            )}
          </>
        )}
      </ul>
      <div className="navbar-user">
        <label className="nav-currency-label" htmlFor="nav-currency">
          <span className="nav-currency-text">Divisa</span>
          <select
            id="nav-currency"
            className="nav-currency-select"
            value={selectedCode}
            disabled={monedasLoading || monedas.length === 0}
            onChange={(e) => setSelectedCode(e.target.value)}
            aria-label="Divisa de visualización y pago"
          >
            {monedas.length === 0 ? (
              <option value="USD">USD</option>
            ) : (
              monedas.map((m) => (
                <option key={m.code} value={m.code}>
                  {m.code} · {m.name}
                </option>
              ))
            )}
          </select>
        </label>
        {isLoggedIn ? (
          <>
            <span className="user-email">{user?.email}</span>
            {user?.roles?.includes('ENTERPRISE') && <span className="user-badge">Empresarial</span>}
            <button type="button" className="btn-logout" onClick={logout}>
              Salir
            </button>
          </>
        ) : (
          <>
            <Link to="/register" className="nav-link">Registrarse</Link>
            <Link to="/login" className="nav-link accent">Iniciar sesión</Link>
          </>
        )}
      </div>
      <div className="navbar-footer">
        <span className="nav-version">v1.0</span>
      </div>
    </nav>
  )
}
