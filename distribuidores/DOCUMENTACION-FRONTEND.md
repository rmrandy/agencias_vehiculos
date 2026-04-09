# Documentación — Frontend Distribuidora

SPA en **React 18**, **TypeScript**, **Vite** y **React Router 7**. Consume el **backend .NET** del mismo monorepo (`distribuidores/backend`).

---

## Stack y requisitos

| Componente | Uso |
|------------|-----|
| React + TS | Vistas y componentes |
| Vite | Dev server y build |
| react-router-dom | Rutas del portal |

Instalación y desarrollo:

```bash
cd distribuidores/frontend
npm install
npm run dev
```

Build de producción:

```bash
npm run build
npm run preview
```

### Documentación HTML (TypeDoc)

Genera un sitio estático a partir de **TypeScript** (`src/`), análogo a Javadoc/DocFX en los otros proyectos:

```bash
cd distribuidores/frontend
npm install
npm run docs
```

Salida: `distribuidores/frontend/docs/_site/index.html` (la carpeta `_site` está en `.gitignore`).

Configuración: `typedoc.json`, `tsconfig.typedoc.json`, portada `docs/index-docs.md`. Recomendado **Node.js 18+**.

---

## Configuración del API

La URL base del backend se define con **`VITE_API_URL`**. Si no existe, el valor por defecto es `http://localhost:5080` (mismo puerto que el backend .NET por defecto).

Archivo `src/api/config.ts`:

```typescript
export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:5080').replace(/\/$/, '')

export async function apiFetch(path: string, options: RequestInit = {}): Promise<any> {
  const url = `${API_URL}${path}`
  const res = await fetch(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  const data = res.status === 204 ? {} : await res.json().catch(() => ({}))
  if (!res.ok) {
    let msg =
      (data as { message?: string }).message || (data as { error?: string }).error || `Error ${res.status}`
    // ... mensaje especial para catálogo unificado
    throw new Error(msg)
  }
  return res.status === 204 ? undefined : data
}
```

Ejemplo `.env` local:

```env
VITE_API_URL=http://localhost:5080
```

**Importante:** el catálogo unificado y la mayoría de rutas `/api/*` corresponden al backend de la distribuidora. No mezclar con el puerto del API Java de la fábrica salvo en pantallas que gestionen explícitamente la URL del proveedor.

---

## Arquitectura de la aplicación

### Punto de entrada

`src/main.tsx` monta `App` en modo estricto.

### Enrutado y providers

`App.tsx` envuelve la app con contextos de autenticación, carrito y toasts; define todas las rutas públicas y de administración:

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { CartProvider } from './context/CartContext'
import { ToastProvider } from './context/ToastContext'
import { Navbar } from './components/Navbar'
// ... vistas

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
                <Route path="/fabricas" element={<ProveedoresAdmin />} />
                <Route path="/fabricas/nuevo" element={<ProveedorForm />} />
                <Route path="/fabricas/editar/:id" element={<ProveedorForm />} />
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
```

(Fuente: `distribuidores/frontend/src/App.tsx`.)

### Capa de API

Los módulos bajo `src/api/` agrupan llamadas por dominio (por ejemplo `repuestos.ts`, `pedidos.ts`, `health.ts`) usando `apiFetch` o `fetch` directo donde haga falta (p. ej. imágenes con `API_IMAGES` en algunas vistas).

---

## Mapa funcional de rutas

| Ruta | Vista | Descripción breve |
|------|--------|-------------------|
| `/` | Home | Inicio |
| `/tienda` | Tienda | Catálogo de compra |
| `/producto/:id` | DetalleProducto | Ficha de repuesto |
| `/login`, `/register` | Login, Register | Autenticación |
| `/carrito`, `/checkout`, `/gracias` | Carrito, Checkout, Gracias | Flujo de compra |
| `/pedidos`, `/pedidos/:orderId` | Mis pedidos | Historial del usuario |
| `/productos`, `/productos/nuevo`, `/productos/editar/:id` | Admin productos | ABM de repuestos |
| `/pedidos-admin` | GestionPedidos | Gestión de pedidos (admin) |
| `/usuarios` | UsuariosAdmin | Usuarios |
| `/fabricas`, `/fabricas/nuevo`, `/fabricas/editar/:id` | Proveedores / fábricas | Configuración de proveedores (URLs de integración) |

---

## Estilos y componentes

- Estilos globales: `src/index.css`, `src/App.css`  
- Componentes reutilizables: `src/components/` (Navbar, modales, etc.)  
- Vistas: `src/views/`  

---

## Referencias en el repositorio

- `distribuidores/frontend/vite.config.ts` — configuración de Vite  
- `distribuidores/frontend/src/api/` — cliente HTTP hacia el backend  
- `distribuidores/frontend/src/context/` — estado de sesión, carrito, notificaciones  
- `distribuidores/DOCUMENTACION-BACKEND.md` — documentación del API consumido  
