<template>
  <div class="reporteria-page">
    <header class="page-header">
      <h1>Reportería</h1>
      <p class="page-subtitle">Log de operaciones: quién, cuándo, archivo usado, exitosos y errores</p>
    </header>

    <div class="tabs">
      <button :class="{ active: activeTab === 'import-export' }" @click="activeTab = 'import-export'">
        Import / Export
      </button>
      <button :class="{ active: activeTab === 'inventario' }" @click="activeTab = 'inventario'">
        Altas de inventario
      </button>
    </div>

    <!-- Tab Import/Export -->
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

    <!-- Tab Inventario -->
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getLogImportExport, getLogInventario } from '../api/reporteria'

const activeTab = ref('import-export')
const logImportExport = ref([])
const logInventario = ref([])
const loadingImportExport = ref(false)
const loadingInventario = ref(false)
const filterOperation = ref('')
const expandedDetail = ref(null)

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
  } catch (e) {
    logImportExport.value = []
  } finally {
    loadingImportExport.value = false
  }
}

async function loadInventario() {
  loadingInventario.value = true
  try {
    logInventario.value = await getLogInventario({ limit: 200 })
  } catch (e) {
    logInventario.value = []
  } finally {
    loadingInventario.value = false
  }
}

onMounted(() => {
  loadImportExport()
  loadInventario()
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
.badge-info { background: #e0f2fe; color: #0369a1; }
.badge-warning { background: #fef3c7; color: #b45309; }
.badge-success { background: #d1fae5; color: #047857; }
.btn-detail {
  background: none;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  font-size: 0.8125rem;
  padding: 0;
}
.btn-detail:hover { text-decoration: underline; }
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
.btn-sm { padding: 6px 12px; font-size: 0.8125rem; }
</style>
