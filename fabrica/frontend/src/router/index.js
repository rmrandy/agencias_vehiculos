import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // Página principal: Tienda (público)
  { path: '/', name: 'Tienda', component: () => import('../views/Tienda.vue'), meta: { title: 'Tienda' } },
  
  // Autenticación
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { title: 'Iniciar sesión', guest: true } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { title: 'Registro', guest: true } },
  
  // Dashboard (solo usuarios autenticados)
  { path: '/dashboard', name: 'Home', component: () => import('../views/Home.vue'), meta: { title: 'Dashboard', requiresAuth: true } },
  
  // Admin
  { path: '/usuarios', name: 'Usuarios', component: () => import('../views/Usuarios.vue'), meta: { title: 'Usuarios', requiresAdmin: true } },
  { path: '/catalogo', name: 'Catalogo', component: () => import('../views/Catalogo.vue'), meta: { title: 'Catálogo', requiresAdmin: true } },
  { path: '/pedidos', name: 'GestionPedidos', component: () => import('../views/GestionPedidos.vue'), meta: { title: 'Gestión de pedidos', requiresAdmin: true } },
  { path: '/reporteria', name: 'Reporteria', component: () => import('../views/Reporteria.vue'), meta: { title: 'Reportería', requiresAdmin: true } },
  
  // Usuario empresarial (compras B2B, perfil con dirección/tarjeta/horario)
  { path: '/mi-perfil-empresarial', name: 'PerfilEmpresarial', component: () => import('../views/PerfilEmpresarial.vue'), meta: { title: 'Mi perfil empresarial', requiresAuth: true } },
  
  // E-commerce
  { path: '/producto/:id', name: 'DetalleProducto', component: () => import('../views/DetalleProducto.vue'), meta: { title: 'Detalle del Producto' } },
  { path: '/carrito', name: 'Carrito', component: () => import('../views/Carrito.vue'), meta: { title: 'Carrito', requiresAuth: true } },
  { path: '/checkout', name: 'Checkout', component: () => import('../views/Checkout.vue'), meta: { title: 'Pago', requiresAuth: true } },
  { path: '/mis-pedidos', name: 'MisPedidos', component: () => import('../views/MisPedidos.vue'), meta: { title: 'Mis Pedidos', requiresAuth: true } },
  { path: '/mis-pedidos/:id', name: 'DetallePedido', component: () => import('../views/DetallePedido.vue'), meta: { title: 'Detalle del Pedido', requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  let user = null
  try {
    const raw = localStorage.getItem('fabrica_user')
    user = raw ? JSON.parse(raw) : null
  } catch {
    user = null
  }
  const isAdmin = user?.roles?.includes('ADMIN')

  // Requiere autenticación
  if (to.meta.requiresAuth && !user) {
    next({ name: 'Login' })
    return
  }

  // Requiere admin
  if (to.meta.requiresAdmin && !isAdmin) {
    next({ name: 'Home' })
    return
  }

  // Perfil empresarial: solo usuarios con rol ENTERPRISE
  if (to.name === 'PerfilEmpresarial') {
    const isEnterprise = user?.roles?.includes('ENTERPRISE')
    if (!user) {
      next({ name: 'Login' })
      return
    }
    if (!isEnterprise) {
      next({ name: 'Home' })
      return
    }
  }

  // Páginas de guest (login/register) - si ya está autenticado, redirigir
  if (to.meta.guest && user) {
    // Si es admin, al dashboard
    if (isAdmin) {
      next({ name: 'Home' })
    } else {
      // Si es usuario normal, a la tienda
      next({ name: 'Tienda' })
    }
    return
  }

  next()
})

export default router
