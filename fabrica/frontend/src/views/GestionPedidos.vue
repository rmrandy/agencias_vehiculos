<template>
  <div class="gestion-pedidos">
    <header class="page-header">
      <h1>📦 Gestión de pedidos</h1>
      <p class="subtitle">Administra pedidos y actualiza su estado. El cliente recibe un correo en cada cambio.</p>
    </header>

    <!-- Filtros -->
    <div class="filters">
      <div class="filter-group">
        <label>Estado</label>
        <select v-model="filters.status">
          <option value="">Todos</option>
          <option v-for="s in statusFlow" :key="s" :value="s">{{ statusLabel(s) }}</option>
          <option value="CANCELLED">Cancelado</option>
        </select>
      </div>
      <div class="filter-group">
        <label>Desde</label>
        <input v-model="filters.from" type="date" />
      </div>
      <div class="filter-group">
        <label>Hasta</label>
        <input v-model="filters.to" type="date" />
      </div>
      <button class="btn btn-primary" @click="loadOrders">Filtrar</button>
    </div>

    <div v-if="loading" class="loading">Cargando pedidos...</div>
    <div v-else-if="error" class="error-msg">{{ error }}</div>

    <div v-else class="table-wrap">
      <table class="orders-table">
        <thead>
          <tr>
            <th>Nº Pedido</th>
            <th>Fecha</th>
            <th>Total</th>
            <th>Estado</th>
            <th>Cambiar a</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in orders" :key="row.order.orderId">
            <td>
              <router-link :to="`/mis-pedidos/${row.order.orderId}`" class="order-link">
                {{ row.order.orderNumber }}
              </router-link>
            </td>
            <td>{{ formatDate(row.order.createdAt) }}</td>
            <td>${{ row.order.total?.toFixed(2) }}</td>
            <td>
              <span class="badge" :class="badgeClass(row.latestStatus?.status)">
                {{ statusLabel(row.latestStatus?.status || '—') }}
              </span>
            </td>
            <td class="cell-actions">
              <template v-if="allowedNextStatuses(row.latestStatus?.status).length > 0">
                <select
                  v-model="selectedStatus[row.order.orderId]"
                  class="status-select"
                  @change="onStatusChange(row.order.orderId, $event)"
                >
                  <option value="">—</option>
                  <option
                    v-for="s in allowedNextStatuses(row.latestStatus?.status)"
                    :key="s"
                    :value="s"
                  >
                    {{ statusLabel(s) }}
                  </option>
                </select>
                <div v-if="selectedStatus[row.order.orderId]" class="extra-fields">
                  <input
                    v-model="commentByOrder[row.order.orderId]"
                    type="text"
                    class="field-comment"
                    placeholder="Comentario (opcional)"
                  />
                  <template v-if="selectedStatus[row.order.orderId] === 'SHIPPED'">
                    <input
                      v-model.number="trackingByOrder[row.order.orderId]"
                      type="text"
                      class="field-tracking"
                      placeholder="Nº seguimiento"
                    />
                    <input
                      v-model.number="etaByOrder[row.order.orderId]"
                      type="number"
                      min="1"
                      class="field-eta"
                      placeholder="ETA (días)"
                    />
                  </template>
                </div>
                <button
                  v-if="selectedStatus[row.order.orderId]"
                  class="btn btn-sm btn-primary"
                  @click="confirmStatusChange(row.order.orderId)"
                >
                  Actualizar
                </button>
              </template>
              <span v-else class="no-more">—</span>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="orders.length === 0" class="empty">No hay pedidos con los filtros aplicados.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuth } from '../composables/useAuth'
import { useToast } from '../composables/useToast'
import { getAllOrders, getOrderStatusFlow, updateOrderStatus } from '../api/pedidos'

const { user } = useAuth()
const { success, error: showError } = useToast()

const orders = ref([])
const statusFlow = ref([])
const loading = ref(true)
const error = ref('')
const filters = ref({ status: '', from: '', to: '' })
const selectedStatus = ref({})
const commentByOrder = ref({})
const trackingByOrder = ref({})
const etaByOrder = ref({})

const statusLabels = {
  INITIATED: 'Iniciada',
  CONFIRMED: 'Confirmado',
  IN_PREPARATION: 'Preparación del pedido',
  PREPARING: 'Preparación del pedido',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado'
}

function statusLabel(s) {
  return statusLabels[s] || s || '—'
}

function allowedNextStatuses(current) {
  if (!current) return []
  const flow = statusFlow.value
  const u = current.toUpperCase()
  const idx = (u === 'CONFIRMED' || u === 'IN_PREPARATION') ? 1 : flow.indexOf(u)
  const next = []
  if (idx >= 0 && idx < flow.length - 1) next.push(flow[idx + 1])
  if (u !== 'DELIVERED') next.push('CANCELLED')
  return next
}

function badgeClass(status) {
  if (!status) return ''
  const u = (status || '').toUpperCase()
  if (u === 'DELIVERED') return 'badge-success'
  if (u === 'CANCELLED') return 'badge-danger'
  if (u === 'SHIPPED') return 'badge-info'
  return 'badge-default'
}

function formatDate(d) {
  if (!d) return '—'
  const date = new Date(d)
  return date.toLocaleDateString('es-GT', { dateStyle: 'short' })
}

async function loadOrders() {
  loading.value = true
  error.value = ''
  try {
    const [list, flow] = await Promise.all([
      getAllOrders({
        status: filters.value.status || undefined,
        from: filters.value.from || undefined,
        to: filters.value.to || undefined
      }),
      getOrderStatusFlow()
    ])
    orders.value = list
    statusFlow.value = flow || []
    selectedStatus.value = {}
    commentByOrder.value = {}
    trackingByOrder.value = {}
    etaByOrder.value = {}
  } catch (e) {
    error.value = e.message || 'Error al cargar pedidos'
  } finally {
    loading.value = false
  }
}

function onStatusChange(orderId, event) {
  const v = event.target?.value
  selectedStatus.value[orderId] = v || null
}

async function confirmStatusChange(orderId) {
  const status = selectedStatus.value[orderId]
  if (!status) return
  const adminId = user.value?.userId
  if (!adminId) {
    showError('Debes iniciar sesión')
    return
  }
  const comment = commentByOrder.value[orderId]?.trim() || ''
  const tracking = status === 'SHIPPED' ? (trackingByOrder.value[orderId]?.toString().trim() || null) : null
  const eta = status === 'SHIPPED' && etaByOrder.value[orderId] != null ? Number(etaByOrder.value[orderId]) : null
  try {
    await updateOrderStatus(orderId, status, comment, adminId, tracking, eta)
    success('Estado actualizado. Se ha enviado un correo al cliente.')
    selectedStatus.value[orderId] = null
    commentByOrder.value[orderId] = ''
    trackingByOrder.value[orderId] = ''
    etaByOrder.value[orderId] = ''
    await loadOrders()
  } catch (e) {
    showError(e.message || 'Error al actualizar estado')
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.gestion-pedidos {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 26px;
  color: #0f172a;
  margin-bottom: 4px;
}

.subtitle {
  color: #64748b;
  font-size: 14px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
  margin-bottom: 24px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-group label {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.filter-group select,
.filter-group input {
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 14px;
  min-width: 140px;
}

.btn {
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  background: #0d9488;
  color: white;
}

.btn:hover {
  background: #0f766e;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
  margin-left: 8px;
}

.loading,
.error-msg {
  padding: 24px;
  text-align: center;
  color: #64748b;
}

.error-msg {
  color: #dc2626;
}

.table-wrap {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.orders-table {
  width: 100%;
  border-collapse: collapse;
}

.orders-table th,
.orders-table td {
  padding: 14px 16px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
}

.orders-table th {
  background: #f8fafc;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.orders-table tr:hover {
  background: #fafafa;
}

.order-link {
  color: #0d9488;
  font-weight: 600;
  text-decoration: none;
}

.order-link:hover {
  text-decoration: underline;
}

.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.badge-default {
  background: #e2e8f0;
  color: #475569;
}

.badge-success {
  background: #d1fae5;
  color: #065f46;
}

.badge-danger {
  background: #fee2e2;
  color: #991b1b;
}

.badge-info {
  background: #dbeafe;
  color: #1e40af;
}

.status-select {
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 13px;
  min-width: 140px;
}

.empty {
  padding: 32px;
  text-align: center;
  color: #64748b;
}

.cell-actions {
  min-width: 260px;
}

.extra-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  align-items: center;
}

.field-comment,
.field-tracking,
.field-eta {
  padding: 6px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 12px;
}

.field-comment {
  flex: 1;
  min-width: 140px;
}

.field-tracking {
  width: 120px;
}

.field-eta {
  width: 70px;
}
</style>
