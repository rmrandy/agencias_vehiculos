# Documentación — Frontend Fábrica

SPA en **Vue 3**, **Vite** y **Vue Router 4**. Consume el **backend Java/Jersey** del mismo proyecto (`fabrica/backend`).

---

## Stack y requisitos

| Componente | Uso |
|------------|-----|
| Vue 3 | Vistas y componentes SFC (`.vue`) |
| vue-router | Rutas, guards y metadatos (`requiresAuth`, `requiresAdmin`, `guest`) |
| Vite | Dev server y build |

Instalación y desarrollo:

```bash
cd fabrica/frontend
npm install
npm run dev
```

Build:

```bash
npm run build
npm run preview
```

### Documentación HTML (TypeDoc, JavaScript)

Genera referencia desde módulos **JS** (`src/api/`, `src/router/`, `src/composables/`, `main.js`). Los `.vue` no entran en TypeDoc; conviene documentar la lógica compartida con **JSDoc**.

```bash
cd fabrica/frontend
npm install
npm run docs
```

Salida: `fabrica/frontend/docs/_site/index.html` ( `_site` en `.gitignore`).

Configuración: `typedoc.json`, `tsconfig.typedoc.json`, portada `docs/index-docs.md`. Recomendado **Node.js 18+**.

---

## Configuración del API

La URL base del backend Java se define con **`VITE_API_URL`**. Por defecto se usa `http://localhost:8080` (alineado con el `PORT` habitual del backend).

`src/api/config.js`:

```javascript
export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export function apiFetch(path, options = {}) {
  const url = `${API_URL}${path}`
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  }
  return fetch(url, { ...options, headers }).then(async (res) => {
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      const msg = data.message || data.error || `Error ${res.status}`
      throw new Error(msg)
    }
    return data
  })
}
```

Ejemplo `.env`:

```env
VITE_API_URL=http://localhost:8080
```

Las rutas del backend están bajo el prefijo **`/api`** (por ejemplo `POST /api/auth/login`, `GET /api/repuestos`).

---

## Arquitectura de la aplicación

### Punto de entrada

`src/main.js` crea la app Vue, registra el router y monta `#app`:

```javascript
import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

createApp(App).use(router).mount('#app')
```

### Router y control de acceso

El archivo `src/router/index.js` define rutas con `meta` para invitados, autenticación, administración y perfil empresarial. Lee el usuario desde `localStorage` bajo la clave **`fabrica_user`**:

```javascript
const routes = [
  { path: '/', name: 'Tienda', component: () => import('../views/Tienda.vue'), meta: { title: 'Tienda' } },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { title: 'Iniciar sesión', guest: true } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { title: 'Registro', guest: true } },
  { path: '/dashboard', name: 'Home', component: () => import('../views/Home.vue'), meta: { title: 'Dashboard', requiresAuth: true } },
  { path: '/usuarios', name: 'Usuarios', component: () => import('../views/Usuarios.vue'), meta: { title: 'Usuarios', requiresAdmin: true } },
  { path: '/catalogo', name: 'Catalogo', component: () => import('../views/Catalogo.vue'), meta: { title: 'Catálogo', requiresAdmin: true } },
  { path: '/pedidos', name: 'GestionPedidos', component: () => import('../views/GestionPedidos.vue'), meta: { title: 'Gestión de pedidos', requiresAdmin: true } },
  { path: '/reporteria', name: 'Reporteria', component: () => import('../views/Reporteria.vue'), meta: { title: 'Reportería', requiresAdmin: true } },
  { path: '/mi-perfil-empresarial', name: 'PerfilEmpresarial', component: () => import('../views/PerfilEmpresarial.vue'), meta: { title: 'Mi perfil empresarial', requiresAuth: true } },
  { path: '/producto/:id', name: 'DetalleProducto', component: () => import('../views/DetalleProducto.vue'), meta: { title: 'Detalle del Producto' } },
  { path: '/carrito', name: 'Carrito', component: () => import('../views/Carrito.vue'), meta: { title: 'Carrito', requiresAuth: true } },
  { path: '/checkout', name: 'Checkout', component: () => import('../views/Checkout.vue'), meta: { title: 'Pago', requiresAuth: true } },
  { path: '/mis-pedidos', name: 'MisPedidos', component: () => import('../views/MisPedidos.vue'), meta: { title: 'Mis Pedidos', requiresAuth: true } },
  { path: '/mis-pedidos/:id', name: 'DetallePedido', component: () => import('../views/DetallePedido.vue'), meta: { title: 'Detalle del Pedido', requiresAuth: true } },
]

router.beforeEach((to, _from, next) => {
  let user = null
  try {
    const raw = localStorage.getItem('fabrica_user')
    user = raw ? JSON.parse(raw) : null
  } catch {
    user = null
  }
  const isAdmin = user?.roles?.includes('ADMIN')
  if (to.meta.requiresAuth && !user) {
    next({ name: 'Login' })
    return
  }
  if (to.meta.requiresAdmin && !isAdmin) {
    next({ name: 'Home' })
    return
  }
  // ... PerfilEmpresarial (rol ENTERPRISE), páginas guest
  next()
})
```

(Fuente: `fabrica/frontend/src/router/index.js` — el archivo completo incluye la rama `PerfilEmpresarial` y la redirección de invitados ya autenticados.)

### Capa de API

Los módulos en `src/api/` (por ejemplo `config.js`, `pedidos.js`, `health.js`) construyen URLs como `` `${API_URL}/api/...` `` y encapsulan `fetch`.

---

## Mapa funcional de rutas

| Ruta | Vista | Acceso |
|------|--------|--------|
| `/` | Tienda | Público |
| `/login`, `/register` | Login, Register | Invitado (redirige si ya hay sesión) |
| `/dashboard` | Home | Autenticado |
| `/usuarios`, `/catalogo`, `/pedidos`, `/reporteria` | Admin | Rol `ADMIN` |
| `/mi-perfil-empresarial` | Perfil empresarial | Autenticado + rol `ENTERPRISE` |
| `/producto/:id` | Detalle | Público |
| `/carrito`, `/checkout` | Compra | Autenticado |
| `/mis-pedidos`, `/mis-pedidos/:id` | Pedidos del usuario | Autenticado |

---

## Componentes y vistas

- `src/App.vue` — layout raíz  
- `src/components/` — Navbar, toasts, subida de imágenes, etc.  
- `src/views/` — pantallas por funcionalidad  

---

## Convivencia con el proyecto Distribuidora

El **frontend de la fábrica** habla con el **backend Java** (puerto típico 8080). El **frontend de la distribuidora** debe usar el **backend .NET** (puerto típico 5080). Mezclar URLs provoca errores 404/405 en endpoints que solo existen en uno u otro servidor.

---

## Referencias en el repositorio

- `fabrica/frontend/src/router/index.js` — rutas y guards  
- `fabrica/frontend/src/api/` — cliente HTTP  
- `fabrica/DOCUMENTACION-BACKEND.md` — documentación del API consumido  
- `fabrica/backend/README.md` — contratos REST y configuración Oracle  
