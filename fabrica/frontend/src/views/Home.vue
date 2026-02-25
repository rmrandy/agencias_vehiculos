<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../composables/useAuth'
import { getHealth } from '../api/health'

const { user, isAdmin } = useAuth()
const health = ref(null)
const loading = ref(true)
const error = ref(null)

onMounted(async () => {
  try {
    health.value = await getHealth()
  } catch (e) {
    error.value = e.message || 'No se pudo conectar al backend'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="dashboard">
    <header class="dashboard-header">
      <h1>Dashboard</h1>
      <p class="dashboard-subtitle">Resumen del sistema Fábrica</p>
    </header>

    <div class="dashboard-grid">
      <section class="card card-welcome">
        <h2 class="card-title">Bienvenido</h2>
        <p class="card-desc">Sistema de gestión para la fábrica. Desde aquí puedes revisar el estado de los servicios y acceder a repuestos.</p>
      </section>

      <section class="card card-health">
        <h2 class="card-title">Estado del backend</h2>
        <div v-if="loading" class="status status-loading">
          <span class="status-dot"></span>
          Comprobando conexión...
        </div>
        <div v-else-if="error" class="status status-error">
          <span class="status-dot"></span>
          {{ error }}
        </div>
        <div v-else class="status status-ok">
          <span class="status-badge">OK</span>
          <span>API en línea — {{ health?.status ?? '—' }}</span>
        </div>
      </section>

      <section class="card card-placeholder">
        <h2 class="card-title">Accesos rápidos</h2>
        <p class="card-desc">Usa el menú lateral para Dashboard, Usuarios (si eres admin), Repuestos, Entrar y Registrarse.</p>
        <div class="quick-links">
          <router-link v-if="isAdmin" to="/usuarios" class="quick-link">Usuarios</router-link>
          <router-link v-if="isAdmin" to="/catalogo" class="quick-link">Catálogo</router-link>
          <a href="#" class="quick-link">Reportes</a>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.dashboard-header {
  margin-bottom: 1.5rem;
}
.dashboard-header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0;
  color: #0f172a;
}
.dashboard-subtitle {
  margin: 0.25rem 0 0;
  color: #64748b;
  font-size: 0.9375rem;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1.25rem;
}
.card {
  background: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: var(--card-shadow);
  padding: 1.5rem;
  border: 1px solid #e2e8f0;
}
.card-welcome { grid-column: 1 / -1; }
.card-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 0.75rem;
  color: #0f172a;
}
.card-desc {
  margin: 0;
  color: #475569;
  font-size: 0.9375rem;
  line-height: 1.6;
}
.status {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: var(--radius-sm);
  font-size: 0.9375rem;
  font-weight: 500;
}
.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.status-loading {
  background: #e0f2fe;
  color: #0369a1;
}
.status-loading .status-dot {
  background: #0ea5e9;
  animation: pulse 1.2s ease-in-out infinite;
}
.status-ok {
  background: #dcfce7;
  color: #166534;
}
.status-ok .status-badge {
  display: inline-flex;
  align-items: center;
  background: #22c55e;
  color: #fff;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
}
.status-error {
  background: #fee2e2;
  color: #b91c1c;
}
.status-error .status-dot { background: #ef4444; }
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .4; }
}
.quick-links {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}
.quick-link {
  display: inline-block;
  padding: 0.5rem 1rem;
  background: #f1f5f9;
  color: #475569;
  text-decoration: none;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  font-weight: 500;
  transition: background .15s, color .15s;
}
.quick-link:hover {
  background: #e2e8f0;
  color: #0f172a;
}
</style>
