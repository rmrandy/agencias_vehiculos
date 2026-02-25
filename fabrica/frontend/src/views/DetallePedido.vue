<template>
  <div class="detalle-pedido-page">
    <div v-if="loading" class="loading">Cargando pedido...</div>

    <div v-else-if="error" class="error-message">{{ error }}</div>

    <div v-else-if="orderData" class="pedido-content">
      <!-- Header -->
      <header class="pedido-header">
        <div>
          <h1>Pedido #{{ orderData.order.orderNumber }}</h1>
          <p class="pedido-date">{{ formatDate(orderData.order.createdAt) }}</p>
        </div>
        <div class="pedido-total">
          <span class="total-label">Total pagado:</span>
          <span class="total-amount">${{ orderData.order.total.toFixed(2) }}</span>
        </div>
      </header>

      <!-- Estado actual -->
      <div class="status-section">
        <h2>Estado del pedido</h2>
        <div v-if="orderData.status" class="current-status">
          <span :class="['status-badge', getStatusClass(orderData.status.status)]">
            {{ getStatusLabel(orderData.status.status) }}
          </span>
          <p v-if="orderData.status.commentText" class="status-comment">
            {{ orderData.status.commentText }}
          </p>
          <p v-if="orderData.status.trackingNumber" class="tracking-number">
            Número de seguimiento: <strong>{{ orderData.status.trackingNumber }}</strong>
          </p>
          <p v-if="orderData.status.etaDays" class="eta">
            Tiempo estimado de entrega: {{ orderData.status.etaDays }} días
          </p>
        </div>
      </div>

      <!-- Items del pedido -->
      <div class="items-section">
        <h2>Artículos</h2>
        <div class="items-list">
          <div v-for="item in orderData.items" :key="item.orderItemId" class="order-item">
            <div class="item-image">
              <img v-if="item.partId" :src="`http://localhost:8080/api/images/part/${item.partId}`" alt="Producto" />
              <div v-else class="no-image">📦</div>
            </div>
            <div class="item-info">
              <h3>Repuesto #{{ item.partId }}</h3>
              <p>Cantidad: {{ item.qty }}</p>
              <p>Precio unitario: ${{ item.unitPrice.toFixed(2) }}</p>
            </div>
            <div class="item-total">
              ${{ item.lineTotal.toFixed(2) }}
            </div>
          </div>
        </div>
      </div>

      <!-- Resumen -->
      <div class="summary-section">
        <div class="summary-row">
          <span>Subtotal:</span>
          <span>${{ orderData.order.subtotal.toFixed(2) }}</span>
        </div>
        <div class="summary-row">
          <span>Envío:</span>
          <span>${{ orderData.order.shippingTotal.toFixed(2) }}</span>
        </div>
        <div class="summary-row total-row">
          <span>Total:</span>
          <span>${{ orderData.order.total.toFixed(2) }}</span>
        </div>
      </div>

      <!-- Botones -->
      <div class="actions-section">
        <router-link to="/mis-pedidos" class="btn btn-secondary">
          Volver a mis pedidos
        </router-link>
        <router-link to="/tienda" class="btn btn-primary">
          Seguir comprando
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderById } from '../api/pedidos'

const route = useRoute()

const orderData = ref(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  const orderId = route.params.id
  try {
    orderData.value = await getOrderById(orderId)
  } catch (e) {
    error.value = e.message || 'Error al cargar el pedido'
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

function getStatusClass(status) {
  const classes = {
    'INITIATED': 'status-initiated',
    'PREPARING': 'status-preparing',
    'SHIPPED': 'status-shipped',
    'DELIVERED': 'status-delivered'
  }
  return classes[status] || 'status-initiated'
}

function getStatusLabel(status) {
  const labels = {
    'INITIATED': 'Iniciado',
    'PREPARING': 'En preparación',
    'SHIPPED': 'Enviado',
    'DELIVERED': 'Entregado'
  }
  return labels[status] || status
}
</script>

<style scoped>
.detalle-pedido-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.loading,
.error-message {
  text-align: center;
  padding: 60px 20px;
}

.error-message {
  color: #ef4444;
}

.pedido-content {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 30px;
}

.pedido-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 20px;
  border-bottom: 2px solid #e5e7eb;
  margin-bottom: 30px;
}

.pedido-header h1 {
  font-size: 28px;
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
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 4px;
}

.total-amount {
  font-size: 32px;
  font-weight: 700;
  color: #10b981;
}

.status-section,
.items-section {
  margin-bottom: 30px;
}

.status-section h2,
.items-section h2 {
  font-size: 20px;
  color: #1f2937;
  margin-bottom: 16px;
}

.current-status {
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
}

.status-badge {
  display: inline-block;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
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

.status-comment,
.tracking-number,
.eta {
  margin-top: 8px;
  font-size: 14px;
  color: #4b5563;
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  display: grid;
  grid-template-columns: 80px 1fr auto;
  gap: 16px;
  align-items: center;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.item-image {
  width: 80px;
  height: 80px;
  background: white;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  font-size: 32px;
  color: #9ca3af;
}

.item-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.item-info p {
  font-size: 14px;
  color: #6b7280;
}

.item-total {
  font-size: 18px;
  font-weight: 700;
  color: #10b981;
}

.summary-section {
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 30px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #e5e7eb;
}

.total-row {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  border-bottom: none;
  padding-top: 16px;
}

.actions-section {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.btn {
  padding: 12px 24px;
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
