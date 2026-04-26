<script setup>
import { useAuth } from '../composables/useAuth'
import { useCart } from '../composables/useCart'
import { useRoute, useRouter } from 'vue-router'
import { useStoreInfo } from '../composables/useStoreInfo'

const { user, isLoggedIn, isAdmin, isEnterprise, logout } = useAuth()
const { cartCount } = useCart()
const { storeInfo } = useStoreInfo()
const route = useRoute()
const router = useRouter()

function isActive(name) {
  return route.name === name
}

function doLogout() {
  logout()
  router.push('/')
}
</script>

<template>
  <nav class="navbar">
    <div class="navbar-brand">
      <router-link to="/" class="brand-link">
        <span class="brand-icon">⚙</span>
        <span class="brand-text">{{ storeInfo.name }}</span>
        <span class="brand-sub">{{ storeInfo.subtitle }}</span>
        <span class="brand-sub">{{ storeInfo.generalInfo }}</span>
      </router-link>
    </div>
    <ul class="nav-links">
      <!-- Tienda (SIEMPRE visible - página principal) -->
      <li>
        <router-link to="/" class="nav-link" :class="{ active: isActive('Tienda') || isActive('DetalleProducto') }">
          <span class="nav-icon">🛍️</span> Tienda
        </router-link>
      </li>

      <!-- Dashboard (solo usuarios autenticados) -->
      <li v-if="isLoggedIn">
        <router-link to="/dashboard" class="nav-link" :class="{ active: isActive('Home') }">
          <span class="nav-icon">◉</span> Dashboard
        </router-link>
      </li>

      <!-- Carrito (solo usuarios autenticados) -->
      <li v-if="isLoggedIn">
        <router-link to="/carrito" class="nav-link" :class="{ active: isActive('Carrito') }">
          <span class="nav-icon">🛒</span> Carrito
          <span v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</span>
        </router-link>
      </li>

      <!-- Mis Pedidos (solo usuarios autenticados) -->
      <li v-if="isLoggedIn">
        <router-link to="/mis-pedidos" class="nav-link" :class="{ active: isActive('MisPedidos') || isActive('DetallePedido') }">
          <span class="nav-icon">📦</span> Mis Pedidos
        </router-link>
      </li>

      <!-- Mi perfil empresarial (solo usuarios empresariales) -->
      <li v-if="isEnterprise">
        <router-link to="/mi-perfil-empresarial" class="nav-link" :class="{ active: isActive('PerfilEmpresarial') }">
          <span class="nav-icon">🏢</span> Perfil empresarial
        </router-link>
      </li>

      <!-- Admin -->
      <li v-if="isAdmin">
        <router-link to="/usuarios" class="nav-link" :class="{ active: isActive('Usuarios') }">
          <span class="nav-icon">▣</span> Usuarios
        </router-link>
      </li>
      <li v-if="isAdmin">
        <router-link to="/catalogo" class="nav-link" :class="{ active: isActive('Catalogo') }">
          <span class="nav-icon">▣</span> Catálogo
        </router-link>
      </li>
      <li v-if="isAdmin">
        <router-link to="/pedidos" class="nav-link" :class="{ active: isActive('GestionPedidos') }">
          <span class="nav-icon">📦</span> Pedidos
        </router-link>
      </li>
      <li v-if="isAdmin">
        <router-link to="/reporteria" class="nav-link" :class="{ active: isActive('Reporteria') }">
          <span class="nav-icon">📋</span> Reportería
        </router-link>
      </li>
      <li v-if="isAdmin">
        <router-link to="/ajustes-tienda" class="nav-link" :class="{ active: isActive('AjustesTienda') }">
          <span class="nav-icon">🛠</span> Ajustes tienda
        </router-link>
      </li>

      <!-- Login/Register (solo visitantes) -->
      <template v-if="!isLoggedIn">
        <li>
          <router-link to="/login" class="nav-link" :class="{ active: isActive('Login') }">
            <span class="nav-icon">→</span> Iniciar sesión
          </router-link>
        </li>
        <li>
          <router-link to="/register" class="nav-link accent" :class="{ active: isActive('Register') }">
            <span class="nav-icon">⊕</span> Registrarse
          </router-link>
        </li>
      </template>
    </ul>
    <div v-if="isLoggedIn" class="navbar-user">
      <span class="user-email">{{ user?.email }}</span>
      <button type="button" class="btn-logout" @click="doLogout">Salir</button>
    </div>
    <div class="navbar-footer">
      <span class="nav-version">v1.0</span>
    </div>
  </nav>
</template>

<style scoped>
.navbar {
  position: fixed;
  left: 0;
  top: 0;
  width: var(--sidebar-width);
  height: 100vh;
  min-height: 100vh;
  background: var(--sidebar-bg);
  color: var(--sidebar-text);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
  z-index: 100;
}
.brand-link {
  text-decoration: none;
  color: inherit;
  display: block;
}
.navbar-brand {
  padding: 1.5rem;
  border-bottom: 1px solid rgba(255,255,255,.06);
}
.brand-icon {
  font-size: 1.75rem;
  display: block;
  margin-bottom: 0.35rem;
}
.brand-text {
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #fff;
}
.brand-sub {
  font-size: 0.75rem;
  color: #94a3b8;
  display: block;
  margin-top: 0.15rem;
}
.nav-links {
  list-style: none;
  margin: 0;
  padding: 1rem 0.75rem;
  flex: 1;
}
.nav-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  color: var(--sidebar-text);
  text-decoration: none;
  border-radius: var(--radius-sm);
  font-size: 0.9375rem;
  font-weight: 500;
  transition: background .15s, color .15s;
}
.nav-link:hover {
  background: var(--sidebar-hover);
  color: #fff;
}
.nav-link.active {
  background: rgba(56, 189, 248, .15);
  color: var(--sidebar-accent);
}
.nav-link.accent {
  color: var(--sidebar-accent);
}
.nav-icon {
  font-size: 0.875rem;
  opacity: .9;
}
.navbar-user {
  padding: 0.75rem 1rem;
  border-top: 1px solid rgba(255,255,255,.06);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.user-email {
  font-size: 0.8125rem;
  color: #94a3b8;
  word-break: break-all;
}
.btn-logout {
  background: transparent;
  border: 1px solid rgba(255,255,255,.2);
  color: var(--sidebar-text);
  padding: 0.4rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: 0.8125rem;
  cursor: pointer;
  transition: background .15s, color .15s;
}
.btn-logout:hover {
  background: var(--sidebar-hover);
  color: #fff;
}
.navbar-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid rgba(255,255,255,.06);
  font-size: 0.75rem;
  color: #64748b;
}

.cart-badge {
  background: #ef4444;
  color: white;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.125rem 0.5rem;
  border-radius: 12px;
  margin-left: auto;
}
</style>
