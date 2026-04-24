<template>
  <div class="reporteria-page">
    <header class="page-header">
      <h1>Reportería</h1>
      <p class="page-subtitle">
        Operaciones (import / inventario) y reportes comerciales (más vendidos, ventas por día, pedidos por canal).
      </p>
    </header>

    <div class="main-tabs">
      <button type="button" :class="{ active: mainSection === 'operaciones' }" @click="mainSection = 'operaciones'">
        Operaciones
      </button>
      <button type="button" :class="{ active: mainSection === 'ventas' }" @click="mainSection = 'ventas'">
        Ventas e ingresos
      </button>
    </div>

    <!-- ——— Operaciones ——— -->
    <template v-if="mainSection === 'operaciones'">
      <div class="tabs">
        <button :class="{ active: activeTab === 'import-export' }" @click="activeTab = 'import-export'">
          Import / Export
        </button>
        <button :class="{ active: activeTab === 'inventario' }" @click="activeTab = 'inventario'">
          Altas de inventario
        </button>
      </div>

      <div v-show="activeTab === 'import-export'" class="tab-content">
        <div class="section-header">
          <h2>Log de importación y exportación</h2>
          <div class="filters">
            <select v-model="filterOperation" @change="loadImportExport">
              <option value="">Todas las operaciones</option>
              <option value="EXPORT">Exportar</option>
              <option value="IMPORT">Importar</option>
              <option value="IMPORT_INVENTORY">Importar inventario</option>
            </select>
            <button type="button" class="btn btn-secondary btn-sm" @click="loadImportExport">Actualizar</button>
          </div>
        </div>
        <div v-if="loadingImportExport" class="loading">Cargando...</div>
        <div v-else class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>Quién</th>
                <th>Cuándo</th>
                <th>Operación</th>
                <th>Archivo usado</th>
                <th>Exitosos</th>
                <th>Errores</th>
                <th>Detalle</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in logImportExport" :key="row.logId">
                <td>{{ row.userDisplayName || '—' }}</td>
                <td>{{ formatDate(row.createdAt) }}</td>
                <td><span class="badge" :class="badgeClass(row.operation)">{{ row.operation }}</span></td>
                <td>{{ row.fileName || '—' }}</td>
                <td>{{ row.successCount }}</td>
                <td>{{ row.errorCount }}</td>
                <td>
                  <button v-if="row.detail" type="button" class="btn-detail" @click="toggleDetail(row.logId)">
                    {{ expandedDetail === row.logId ? 'Ocultar' : 'Ver' }}
                  </button>
                  <span v-else>—</span>
                  <pre v-if="expandedDetail === row.logId && row.detail" class="detail-pre">{{ row.detail }}</pre>
                </td>
              </tr>
              <tr v-if="logImportExport.length === 0">
                <td colspan="7" class="empty">No hay registros</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-show="activeTab === 'inventario'" class="tab-content">
        <div class="section-header">
          <h2>Log de altas de inventario</h2>
          <button type="button" class="btn btn-secondary btn-sm" @click="loadInventario">Actualizar</button>
        </div>
        <div v-if="loadingInventario" class="loading">Cargando...</div>
        <div v-else class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>Quién</th>
                <th>Cuándo</th>
                <th>Repuesto</th>
                <th>Código</th>
                <th>Cantidad agregada</th>
                <th>Stock anterior</th>
                <th>Stock nuevo</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in logInventario" :key="row.logId">
                <td>{{ row.userDisplayName || '—' }}</td>
                <td>{{ formatDate(row.createdAt) }}</td>
                <td>{{ row.partTitle || '—' }}</td>
                <td><code>{{ row.partNumber || '—' }}</code></td>
                <td>{{ row.quantityAdded }}</td>
                <td>{{ row.previousQuantity }}</td>
                <td>{{ row.newQuantity }}</td>
              </tr>
              <tr v-if="logInventario.length === 0">
                <td colspan="7" class="empty">No hay registros</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>

    <!-- ——— Ventas e ingresos ——— -->
    <template v-else>
      <div class="comercial-filters">
        <label>Desde <input v-model="fromDate" type="date" /></label>
        <label>Hasta <input v-model="toDate" type="date" /></label>
        <button type="button" class="btn btn-primary btn-sm" @click="loadComercial">Actualizar</button>
      </div>
      <div v-if="loadingComercial" class="loading">Cargando reportes…</div>
      <template v-else>
        <section class="comercial-block">
          <h2>Repuestos más vendidos</h2>
          <p class="hint">Unidades en pedidos cuyo último estado no es cancelado.</p>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Código</th>
                  <th>Descripción</th>
                  <th>Unidades</th>
                  <th>Importe</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="masVendidos.length === 0">
                  <td colspan="5" class="empty">Sin datos en el período.</td>
                </tr>
                <tr v-for="(r, idx) in masVendidos" :key="r.partId">
                  <td>{{ idx + 1 }}</td>
                  <td>
                    <router-link :to="`/producto/${r.partId}`">{{ r.partNumber || '—' }}</router-link>
                  </td>
                  <td>{{ r.partTitle || '—' }}</td>
                  <td>{{ r.totalQty }}</td>
                  <td>{{ money(r.totalImporte) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="comercial-block">
          <h2>Ventas por día</h2>
          <p class="hint">Pedidos no cancelados: cantidad e importe total por día.</p>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Pedidos</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="ventasDiarias.length === 0">
                  <td colspan="3" class="empty">Sin datos.</td>
                </tr>
                <tr v-for="r in ventasDiarias" :key="r.fecha">
                  <td>{{ formatDay(r.fecha) }}</td>
                  <td>{{ r.pedidoCount }}</td>
                  <td>{{ money(r.totalImporte) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="comercial-block">
          <h2>Pedidos por canal</h2>
          <p class="hint">Tienda fábrica frente a integración distribuidora (excluye cancelados).</p>
          <div class="table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Origen</th>
                  <th>Pedidos</th>
                  <th>Total</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="porOrigen.length === 0">
                  <td colspan="3" class="empty">Sin datos.</td>
                </tr>
                <tr v-for="r in porOrigen" :key="r.orderOrigin">
                  <td>{{ originLabel(r.orderOrigin) }}</td>
                  <td>{{ r.pedidoCount }}</td>
                  <td>{{ money(r.totalImporte) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  getLogImportExport,
  getLogInventario,
  getMasVendidos,
  getVentasDiarias,
  getPedidosPorOrigen,
  defaultReportDateRange,
} from '../api/reporteria'

const mainSection = ref('operaciones')
const activeTab = ref('import-export')
const logImportExport = ref([])
const logInventario = ref([])
const loadingImportExport = ref(false)
const loadingInventario = ref(false)
const filterOperation = ref('')
const expandedDetail = ref(null)

const dr = defaultReportDateRange()
const fromDate = ref(dr.from)
const toDate = ref(dr.to)
const loadingComercial = ref(false)
const masVendidos = ref([])
const ventasDiarias = ref([])
const porOrigen = ref([])

function money(v) {
  const n = Number(v)
  if (Number.isNaN(n)) return '—'
  return new Intl.NumberFormat('es-GT', { style: 'currency', currency: 'USD' }).format(n)
}

function formatDay(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleDateString('es-GT', { dateStyle: 'medium' })
  } catch {
    return iso
  }
}

function originLabel(o) {
  if (!o || o === 'FABRICA_WEB') return 'Tienda fábrica'
  if (o === 'DISTRIBUIDORA') return 'Distribuidora'
  return o
}

function formatDate(d) {
  if (!d) return '—'
  return new Date(d).toLocaleString('es', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function badgeClass(op) {
  if (op === 'EXPORT') return 'badge-info'
  if (op === 'IMPORT') return 'badge-warning'
  if (op === 'IMPORT_INVENTORY') return 'badge-success'
  return ''
}

function toggleDetail(logId) {
  expandedDetail.value = expandedDetail.value === logId ? null : logId
}

async function loadImportExport() {
  loadingImportExport.value = true
  try {
    logImportExport.value = await getLogImportExport({
      limit: 200,
      operation: filterOperation.value || undefined,
    })
  } catch {
    logImportExport.value = []
  } finally {
    loadingImportExport.value = false
  }
}

async function loadInventario() {
  loadingInventario.value = true
  try {
    logInventario.value = await getLogInventario({ limit: 200 })
  } catch {
    logInventario.value = []
  } finally {
    loadingInventario.value = false
  }
}

async function loadComercial() {
  loadingComercial.value = true
  try {
    const [mv, vd, po] = await Promise.all([
      getMasVendidos({ from: fromDate.value, to: toDate.value, limit: 30 }),
      getVentasDiarias({ from: fromDate.value, to: toDate.value }),
      getPedidosPorOrigen({ from: fromDate.value, to: toDate.value }),
    ])
    masVendidos.value = Array.isArray(mv) ? mv : []
    ventasDiarias.value = Array.isArray(vd) ? vd : []
    porOrigen.value = Array.isArray(po) ? po : []
  } catch {
    masVendidos.value = []
    ventasDiarias.value = []
    porOrigen.value = []
  } finally {
    loadingComercial.value = false
  }
}

onMounted(() => {
  loadImportExport()
  loadInventario()
  loadComercial()
})
</script>

<style scoped>
.reporteria-page {
  max-width: 100%;
}
.page-header {
  margin-bottom: 1.5rem;
}
.page-header h1 {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
  color: #0f172a;
}
.page-subtitle {
  margin: 0.25rem 0 0;
  color: #64748b;
  font-size: 0.9375rem;
}
.main-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.main-tabs button {
  padding: 0.65rem 1.1rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  cursor: pointer;
}
.main-tabs button.active {
  background: #0d9488;
  color: #fff;
  border-color: #0d9488;
}
.comercial-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 1rem;
}
.comercial-filters label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}
.comercial-filters input[type='date'] {
  padding: 8px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}
.comercial-block {
  margin-top: 1.75rem;
}
.comercial-block h2 {
  font-size: 1.125rem;
  margin: 0 0 6px;
  color: #0f172a;
}
.hint {
  font-size: 0.8125rem;
  color: #64748b;
  margin: 0 0 10px;
}
.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 2px solid #e2e8f0;
}
.tabs button {
  padding: 0.75rem 1.25rem;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  color: #64748b;
  font-weight: 500;
  cursor: pointer;
}
.tabs button.active {
  color: var(--sidebar-accent);
  border-bottom-color: var(--sidebar-accent);
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.section-header h2 {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
  color: #0f172a;
}
.filters {
  display: flex;
  gap: 8px;
  align-items: center;
}
.table-wrap {
  overflow-x: auto;
  background: var(--card-bg);
  border-radius: var(--radius);
  border: 1px solid #e2e8f0;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}
.data-table th,
.data-table td {
  padding: 0.75rem 1rem;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}
.data-table th {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
}
.data-table td.empty {
  color: #94a3b8;
  text-align: center;
  padding: 2rem;
}
.badge {
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}
.badge-info {
  background: #e0f2fe;
  color: #0369a1;
}
.badge-warning {
  background: #fef3c7;
  color: #b45309;
}
.badge-success {
  background: #d1fae5;
  color: #047857;
}
.btn-detail {
  background: none;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  font-size: 0.8125rem;
  padding: 0;
}
.btn-detail:hover {
  text-decoration: underline;
}
.detail-pre {
  margin: 0.5rem 0 0;
  padding: 0.75rem;
  background: #f1f5f9;
  border-radius: 6px;
  font-size: 0.75rem;
  white-space: pre-wrap;
  max-width: 400px;
  max-height: 200px;
  overflow: auto;
}
.loading {
  padding: 2rem;
  text-align: center;
  color: #64748b;
}
.btn-sm {
  padding: 6px 12px;
  font-size: 0.8125rem;
}
.data-table a {
  color: #0d9488;
  font-weight: 600;
  text-decoration: none;
}
.data-table a:hover {
  text-decoration: underline;
}
</style>
