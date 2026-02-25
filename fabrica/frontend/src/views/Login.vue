<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { useToast } from '../composables/useToast'
import { login } from '../api/auth'

const router = useRouter()
const route = useRoute()
const { setUser, isAdmin } = useAuth()
const { success } = useToast()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function onSubmit() {
  error.value = ''
  if (!email.value.trim() || !password.value) {
    error.value = 'Email y contraseña son obligatorios'
    return
  }
  loading.value = true
  try {
    const user = await login(email.value.trim(), password.value)
    setUser(user)
    success(`¡Bienvenido, ${user.email}!`)
    
    // Redirigir según el parámetro redirect o el rol del usuario
    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else if (isAdmin.value) {
      router.push({ name: 'Home' }) // Dashboard para admin
    } else {
      router.push({ name: 'Tienda' }) // Tienda para usuarios normales
    }
  } catch (e) {
    error.value = e.message || 'Credenciales incorrectas'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="auth-title">Iniciar sesión</h1>
      <p class="auth-subtitle">Fábrica — Agencias Vehículos</p>

      <form @submit.prevent="onSubmit" class="auth-form">
        <div v-if="error" class="auth-error">{{ error }}</div>
        <div class="form-group">
          <label for="email">Email</label>
          <input id="email" v-model="email" type="email" required autocomplete="email" placeholder="tu@email.com" />
        </div>
        <div class="form-group">
          <label for="password">Contraseña</label>
          <input id="password" v-model="password" type="password" required autocomplete="current-password" placeholder="••••••••" />
        </div>
        <button type="submit" class="btn btn-primary" :disabled="loading">
          {{ loading ? 'Entrando…' : 'Entrar' }}
        </button>
      </form>

      <p class="auth-footer">
        ¿No tienes cuenta? <router-link to="/register">Regístrate</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 2rem;
}
.auth-card {
  width: 100%;
  max-width: 400px;
  background: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: var(--card-shadow);
  padding: 2rem;
  border: 1px solid #e2e8f0;
}
.auth-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 0.25rem;
  color: #0f172a;
}
.auth-subtitle {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0 0 1.5rem;
}
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.auth-error {
  padding: 0.75rem 1rem;
  background: #fee2e2;
  color: #b91c1c;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.form-group label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}
.form-group input {
  padding: 0.6rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: var(--radius-sm);
  font-size: 1rem;
}
.form-group input:focus {
  outline: none;
  border-color: var(--sidebar-accent);
  box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.2);
}
.btn {
  padding: 0.75rem 1.25rem;
  font-size: 1rem;
  font-weight: 600;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.15s;
}
.btn-primary {
  background: var(--sidebar-bg);
  color: #fff;
  margin-top: 0.5rem;
}
.btn-primary:hover:not(:disabled) {
  background: var(--sidebar-hover);
}
.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.auth-footer {
  margin: 1.5rem 0 0;
  font-size: 0.875rem;
  color: #64748b;
  text-align: center;
}
.auth-footer a {
  color: var(--sidebar-accent);
  font-weight: 500;
  text-decoration: none;
}
.auth-footer a:hover {
  text-decoration: underline;
}
</style>
