<template>
  <div class="pedidos-page">
    <header class="page-header">
      <h1>📦 Mis Pedidos</h1>
      <p class="page-subtitle">Historial de compras y seguimiento</p>
    </header>

    <div v-if="loading" class="loading">Cargando pedidos...</div>

    <div v-else-if="error" class="error-message">{{ error }}</div>

    <div v-else-if="pedidos.length === 0" class="empty-state">
      <p>No tienes pedidos aún</p>
      <router-link to="/tienda" class="btn btn-primary">Ir a la tienda</router-link>
    </div>

    <div v-else class="pedidos-list">
      <div v-for="pedido in pedidos" :key="pedido.orderId" class="pedido-card">
        <div class="pedido-header">
          <div>
            <h3>Pedido #{{ pedido.orderNumber }}</h3>
            <p class="pedido-date">{{ formatDate(pedido.createdAt) }}</p>
          </div>
          <div class="pedido-total">
            <span class="total-label">Total:</span>
            <span class="total-amount">${{ pedido.total.toFixed(2) }}</span>
          </div>
        </div>

        <div class="pedido-status">
          <span :class="['status-badge', getStatusClass(pedido.orderId)]">
            {{ getStatusLabel(pedido.orderId) }}
          </span>
        </div>

        <div class="pedido-actions">
          <router-link :to="`/mis-pedidos/${pedido.orderId}`" class="btn btn-secondary">
            Ver detalles
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuth } from '../composables/useAuth'
import { getUserOrders } from '../api/pedidos'

const { user } = useAuth()

const pedidos = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  if (!user.value) {
    error.value = 'Debes iniciar sesión'
    loading.value = false
    return
  }

  try {
    pedidos.value = await getUserOrders(user.value.userId)
  } catch (e) {
    error.value = e.message || 'Error al cargar pedidos'
  } finally {
    loading.value = false
  }
})

function formatDate(dateString) {
  const date = new Date(dateString)
  return date.toLocaleDateString('es-ES', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getStatusClass(orderId) {
  // Por ahora retornamos un estado por defecto
  // En la vista de detalle cargaremos el estado real
  return 'status-initiated'
}

function getStatusLabel(orderId) {
  return 'Iniciado'
}
</script>

<style scoped>
.pedidos-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 32px;
  color: #1f2937;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #6b7280;
  font-size: 16px;
}

.loading,
.error-message,
.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.error-message {
  color: #ef4444;
}

.empty-state p {
  font-size: 18px;
  color: #6b7280;
  margin-bottom: 20px;
}

.pedidos-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pedido-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
}

.pedido-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.pedido-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.pedido-date {
  font-size: 14px;
  color: #6b7280;
}

.pedido-total {
  text-align: right;
}

.total-label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.total-amount {
  font-size: 24px;
  font-weight: 700;
  color: #10b981;
}

.pedido-status {
  margin-bottom: 16px;
}

.status-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-initiated {
  background: #dbeafe;
  color: #1e40af;
}

.status-preparing {
  background: #fef3c7;
  color: #92400e;
}

.status-shipped {
  background: #e0e7ff;
  color: #4338ca;
}

.status-delivered {
  background: #d1fae5;
  color: #065f46;
}

.pedido-actions {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
  transition: all 0.2s;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}
</style>
