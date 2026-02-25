import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { CartProvider } from './context/CartContext'
import { ToastProvider } from './context/ToastContext'
import { Navbar } from './components/Navbar'
import { Home } from './views/Home'
import { Login } from './views/Login'
import { Register } from './views/Register'
import { Tienda } from './views/Tienda'
import { Carrito } from './views/Carrito'
import { Checkout } from './views/Checkout'
import { MisPedidos } from './views/MisPedidos'
import { DetallePedido } from './views/DetallePedido'
import { ProductosAdmin } from './views/ProductosAdmin'
import { ProductoForm } from './views/ProductoForm'
import { DetalleProducto } from './views/DetalleProducto'
import { Gracias } from './views/Gracias'
import { GestionPedidos } from './views/GestionPedidos'
import { UsuariosAdmin } from './views/UsuariosAdmin'
import './App.css'

function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <ToastProvider>
        <BrowserRouter>
          <div className="app-layout">
            <Navbar />
            <main className="main-content">
              <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/tienda" element={<Tienda />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route path="/carrito" element={<Carrito />} />
                <Route path="/checkout" element={<Checkout />} />
                <Route path="/pedidos" element={<MisPedidos />} />
                <Route path="/pedidos/:orderId" element={<DetallePedido />} />
                <Route path="/productos" element={<ProductosAdmin />} />
                <Route path="/productos/nuevo" element={<ProductoForm />} />
                <Route path="/productos/editar/:id" element={<ProductoForm />} />
                <Route path="/pedidos-admin" element={<GestionPedidos />} />
                <Route path="/usuarios" element={<UsuariosAdmin />} />
                <Route path="/producto/:id" element={<DetalleProducto />} />
                <Route path="/gracias" element={<Gracias />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </main>
          </div>
        </BrowserRouter>
        </ToastProvider>
      </CartProvider>
    </AuthProvider>
  )
}

export default App
