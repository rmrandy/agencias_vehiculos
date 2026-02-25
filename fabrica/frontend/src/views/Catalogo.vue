<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from '../composables/useToast'
import { useAuth } from '../composables/useAuth'
import { 
  listCategorias, listMarcas, listRepuestos, 
  createCategoria, createMarca, createRepuesto,
  updateRepuesto, deleteRepuesto as deleteRepuestoApi,
  buscarRepuestos, exportRepuestos, importRepuestos,
  importInventarioRepuestos, addInventarioRepuesto
} from '../api/catalogo'
import ImageUpload from '../components/ImageUpload.vue'

const { success, error: showError } = useToast()
const { user } = useAuth()
const activeTab = ref('repuestos')
const categorias = ref([])
const marcas = ref([])
const repuestos = ref([])
const loading = ref(true)
const error = ref('')
const searchForm = ref({ nombre: '', descripcion: '', especificaciones: '' })
const searchLoading = ref(false)
const isSearchResult = ref(false)
const showInventarioModal = ref(false)
const inventarioRepuesto = ref(null)
const inventarioCantidad = ref(10)
const inventarioSaving = ref(false)
const importFileInput = ref(null)
const importInventarioFileInput = ref(null)
const importResult = ref(null)

// Formularios
const showCategoriaForm = ref(false)
const showMarcaForm = ref(false)
const showRepuestoForm = ref(false)

const categoriaForm = ref({ name: '', parentId: null, imageData: null, imageType: null })
const marcaForm = ref({ name: '', imageData: null, imageType: null })
const repuestoForm = ref({
  categoryId: null,
  brandId: null,
  partNumber: '',
  title: '',
  description: '',
  weightLb: null,
  price: null,
  stockQuantity: 0,
  lowStockThreshold: 5,
  imageData: null,
  imageType: null,
})

const editingRepuesto = ref(null)

onMounted(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [c, m, r] = await Promise.all([listCategorias(), listMarcas(), listRepuestos()])
    categorias.value = c
    marcas.value = m
    repuestos.value = r
  } catch (e) {
    error.value = e.message || 'Error al cargar datos'
  } finally {
    loading.value = false
  }
}

async function submitCategoria() {
  try {
    const payload = { ...categoriaForm.value }
    // Si hay imagen, extraer solo el base64 sin el prefijo data:
    if (payload.imageData?.imageData) {
      const imgData = payload.imageData.imageData
      const imgType = payload.imageData.imageType
      payload.imageData = imgData
      payload.imageType = imgType
    }
    await createCategoria(payload)
    success(`Categoría "${categoriaForm.value.name}" creada exitosamente`)
    categoriaForm.value = { name: '', parentId: null, imageData: null, imageType: null }
    showCategoriaForm.value = false
    await loadData()
  } catch (e) {
    showError(e.message || 'Error al crear categoría')
  }
}

async function submitMarca() {
  try {
    const payload = { ...marcaForm.value }
    // Si hay imagen, extraer solo el base64 sin el prefijo data:
    if (payload.imageData?.imageData) {
      const imgData = payload.imageData.imageData
      const imgType = payload.imageData.imageType
      payload.imageData = imgData
      payload.imageType = imgType
    }
    await createMarca(payload)
    success(`Marca "${marcaForm.value.name}" creada exitosamente`)
    marcaForm.value = { name: '', imageData: null, imageType: null }
    showMarcaForm.value = false
    await loadData()
  } catch (e) {
    showError(e.message || 'Error al crear marca')
  }
}

async function submitRepuesto() {
  try {
    const data = {
      ...repuestoForm.value,
      categoryId: Number(repuestoForm.value.categoryId),
      brandId: Number(repuestoForm.value.brandId),
      weightLb: repuestoForm.value.weightLb ? Number(repuestoForm.value.weightLb) : null,
      price: Number(repuestoForm.value.price),
      stockQuantity: Number(repuestoForm.value.stockQuantity) || 0,
      lowStockThreshold: Number(repuestoForm.value.lowStockThreshold) || 5,
    }
    // Si hay imagen, extraer solo el base64 sin el prefijo data:
    if (data.imageData?.imageData) {
      const imgData = data.imageData.imageData
      const imgType = data.imageData.imageType
      data.imageData = imgData
      data.imageType = imgType
    }
    
    if (editingRepuesto.value) {
      // Actualizar
      await updateRepuesto(editingRepuesto.value.partId, data)
      success(`Repuesto "${repuestoForm.value.title}" actualizado exitosamente`)
    } else {
      // Crear
      await createRepuesto(data)
      success(`Repuesto "${repuestoForm.value.title}" creado exitosamente`)
    }
    
    resetRepuestoForm()
    await loadData()
  } catch (e) {
    showError(e.message || 'Error al guardar repuesto')
  }
}

function resetRepuestoForm() {
  repuestoForm.value = { 
    categoryId: null, 
    brandId: null, 
    partNumber: '', 
    title: '', 
    description: '', 
    weightLb: null, 
    price: null,
    stockQuantity: 0,
    lowStockThreshold: 5,
    imageData: null, 
    imageType: null 
  }
  editingRepuesto.value = null
  showRepuestoForm.value = false
}

function editRepuesto(repuesto) {
  editingRepuesto.value = repuesto
  repuestoForm.value = {
    categoryId: repuesto.categoryId,
    brandId: repuesto.brandId,
    partNumber: repuesto.partNumber,
    title: repuesto.title,
    description: repuesto.description || '',
    weightLb: repuesto.weightLb,
    price: repuesto.price,
    stockQuantity: repuesto.stockQuantity || 0,
    lowStockThreshold: repuesto.lowStockThreshold || 5,
    imageData: null,
    imageType: null,
  }
  showRepuestoForm.value = true
  // Scroll to form
  setTimeout(() => {
    document.querySelector('.form-card')?.scrollIntoView({ behavior: 'smooth' })
  }, 100)
}

async function deleteRepuesto(repuesto) {
  if (!confirm(`¿Estás seguro de eliminar el repuesto "${repuesto.title}"?`)) {
    return
  }
  try {
    await deleteRepuestoApi(repuesto.partId)
    success(`Repuesto "${repuesto.title}" eliminado exitosamente`)
    await loadData()
  } catch (e) {
    showError(e.message || 'Error al eliminar repuesto')
  }
}

async function runBusqueda() {
  const { nombre, descripcion, especificaciones } = searchForm.value
  if (!nombre?.trim() && !descripcion?.trim() && !especificaciones?.trim()) {
    showError('Indica al menos un criterio de búsqueda')
    return
  }
  searchLoading.value = true
  try {
    repuestos.value = await buscarRepuestos({
      nombre: nombre?.trim() || undefined,
      descripcion: descripcion?.trim() || undefined,
      especificaciones: especificaciones?.trim() || undefined,
    })
    isSearchResult.value = true
    success(`Encontrados ${repuestos.value.length} repuestos`)
  } catch (e) {
    showError(e.message || 'Error en la búsqueda')
  } finally {
    searchLoading.value = false
  }
}

function clearBusqueda() {
  searchForm.value = { nombre: '', descripcion: '', especificaciones: '' }
  isSearchResult.value = false
  loadData()
}

async function doExportRepuestos() {
  try {
    const data = await exportRepuestos(user.value?.userId)
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `repuestos-${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
    success(`Exportados ${data.length} repuestos`)
  } catch (e) {
    showError(e.message || 'Error al exportar')
  }
}

function triggerImportFile() {
  importResult.value = null
  importFileInput.value?.click()
}

async function onImportFile(e) {
  const file = e.target?.files?.[0]
  if (!file) return
  importResult.value = null
  try {
    const text = await file.text()
    const data = JSON.parse(text)
    const items = Array.isArray(data) ? data : (data.items || [])
    const res = await importRepuestos({
      userId: user.value?.userId,
      fileName: file.name,
      items,
    })
    importResult.value = res
    success(`Importación: ${res.successCount} ok, ${res.errorCount} errores`)
    await loadData()
  } catch (err) {
    showError(err.message || 'Error al importar JSON')
  }
  e.target.value = ''
}

function triggerImportInventarioFile() {
  importResult.value = null
  importInventarioFileInput.value?.click()
}

async function onImportInventarioFile(e) {
  const file = e.target?.files?.[0]
  if (!file) return
  importResult.value = null
  try {
    const text = await file.text()
    const data = JSON.parse(text)
    const items = Array.isArray(data) ? data : (data.items || [])
    const res = await importInventarioRepuestos({
      userId: user.value?.userId,
      fileName: file.name,
      items,
    })
    importResult.value = res
    success(`Inventario: ${res.successCount} ok, ${res.errorCount} errores`)
    await loadData()
  } catch (err) {
    showError(err.message || 'Error al importar inventario')
  }
  e.target.value = ''
}

function openInventarioModal(repuesto) {
  inventarioRepuesto.value = repuesto
  inventarioCantidad.value = 10
  showInventarioModal.value = true
}

function closeInventarioModal() {
  showInventarioModal.value = false
  inventarioRepuesto.value = null
}

async function submitInventario() {
  if (!inventarioRepuesto.value || !user.value?.userId || inventarioCantidad.value < 1) return
  inventarioSaving.value = true
  try {
    await addInventarioRepuesto(inventarioRepuesto.value.partId, {
      userId: user.value.userId,
      cantidad: inventarioCantidad.value,
    })
    success(`Se agregaron ${inventarioCantidad.value} unidades a "${inventarioRepuesto.value.title}"`)
    closeInventarioModal()
    await loadData()
  } catch (e) {
    showError(e.message || 'Error al agregar inventario')
  } finally {
    inventarioSaving.value = false
  }
}
</script>

<template>
  <div class="catalogo-page">
    <header class="page-header">
      <h1>Catálogo</h1>
      <p class="page-subtitle">Administrar categorías, marcas, vehículos y repuestos</p>
    </header>

    <div class="tabs">
      <button :class="{ active: activeTab === 'repuestos' }" @click="activeTab = 'repuestos'">Repuestos</button>
      <button :class="{ active: activeTab === 'categorias' }" @click="activeTab = 'categorias'">Categorías</button>
      <button :class="{ active: activeTab === 'marcas' }" @click="activeTab = 'marcas'">Marcas</button>
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-else-if="loading" class="loading">Cargando catálogo…</div>

    <!-- TAB: Repuestos -->
    <div v-show="activeTab === 'repuestos'" class="tab-content">
      <div class="section-header">
        <h2>Repuestos</h2>
        <div class="header-actions">
          <button class="btn btn-secondary btn-sm" @click="doExportRepuestos" title="Exportar a JSON">📤 Exportar JSON</button>
          <button class="btn btn-secondary btn-sm" @click="triggerImportFile" title="Importar repuestos (sobreescribe)">📥 Importar JSON</button>
          <button class="btn btn-secondary btn-sm" @click="triggerImportInventarioFile" title="Carga masiva de inventario">📦 Importar inventario</button>
          <button class="btn btn-primary" @click="showRepuestoForm = !showRepuestoForm">
            {{ showRepuestoForm ? 'Cancelar' : '+ Nuevo repuesto' }}
          </button>
        </div>
      </div>
      <input ref="importFileInput" type="file" accept=".json,application/json" class="hidden" @change="onImportFile" />
      <input ref="importInventarioFileInput" type="file" accept=".json,application/json" class="hidden" @change="onImportInventarioFile" />
      <div v-if="importResult" class="import-result">
        <strong>Resultado:</strong> {{ importResult.successCount }} exitosos, {{ importResult.errorCount }} errores.
        <pre v-if="importResult.detail" class="import-detail">{{ importResult.detail }}</pre>
        <button type="button" class="btn btn-sm" @click="importResult = null">Cerrar</button>
      </div>

      <div class="search-card">
        <h3>Buscar repuestos</h3>
        <p class="search-hint">Nombre, descripción o especificaciones (servicio de búsqueda REST)</p>
        <div class="search-row">
          <input v-model="searchForm.nombre" placeholder="Nombre" class="search-input" />
          <input v-model="searchForm.descripcion" placeholder="Descripción" class="search-input" />
          <input v-model="searchForm.especificaciones" placeholder="Especificaciones" class="search-input" />
          <button type="button" class="btn btn-primary" :disabled="searchLoading" @click="runBusqueda">Buscar</button>
          <button v-if="isSearchResult" type="button" class="btn btn-secondary" @click="clearBusqueda">Ver todos</button>
        </div>
      </div>

      <div v-if="showRepuestoForm" class="form-card">
        <h3>{{ editingRepuesto ? 'Editar' : 'Nuevo' }} repuesto</h3>
        <form @submit.prevent="submitRepuesto" class="form-grid">
          <div class="form-group">
            <label>Categoría *</label>
            <select v-model="repuestoForm.categoryId" required>
              <option :value="null">Selecciona</option>
              <option v-for="c in categorias" :key="c.categoryId" :value="c.categoryId">{{ c.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>Marca *</label>
            <select v-model="repuestoForm.brandId" required>
              <option :value="null">Selecciona</option>
              <option v-for="m in marcas" :key="m.brandId" :value="m.brandId">{{ m.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>Número de parte *</label>
            <input v-model="repuestoForm.partNumber" required placeholder="ABC-123" :disabled="!!editingRepuesto" />
          </div>
          <div class="form-group">
            <label>Título *</label>
            <input v-model="repuestoForm.title" required placeholder="Filtro de aceite" />
          </div>
          <div class="form-group full-width">
            <label>Descripción</label>
            <textarea v-model="repuestoForm.description" rows="3" placeholder="Descripción del repuesto"></textarea>
          </div>
          <div class="form-group">
            <label>Peso (lb)</label>
            <input v-model="repuestoForm.weightLb" type="number" step="0.01" placeholder="0.5" />
          </div>
          <div class="form-group">
            <label>Precio *</label>
            <input v-model="repuestoForm.price" type="number" step="0.01" required placeholder="25.99" />
          </div>
          
          <!-- CAMPOS DE INVENTARIO -->
          <div class="form-group">
            <label>Stock disponible *</label>
            <input v-model="repuestoForm.stockQuantity" type="number" min="0" required placeholder="100" />
          </div>
          <div class="form-group">
            <label>Umbral bajo stock *</label>
            <input v-model="repuestoForm.lowStockThreshold" type="number" min="1" required placeholder="5" />
            <small>Se mostrará alerta cuando el stock sea menor o igual a este valor</small>
          </div>
          
          <div class="form-group full-width">
            <label>Imagen {{ editingRepuesto ? '(dejar vacío para mantener actual)' : '(opcional)' }}</label>
            <ImageUpload v-model="repuestoForm.imageData" />
          </div>
          <div class="form-actions full-width">
            <button type="button" class="btn btn-secondary" @click="resetRepuestoForm">Cancelar</button>
            <button type="submit" class="btn btn-primary">
              {{ editingRepuesto ? 'Actualizar' : 'Crear' }} repuesto
            </button>
          </div>
        </form>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Número</th>
              <th>Título</th>
              <th>Categoría</th>
              <th>Marca</th>
              <th>Precio</th>
              <th>Stock</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in repuestos" :key="r.partId">
              <td>{{ r.partId }}</td>
              <td><code>{{ r.partNumber }}</code></td>
              <td>{{ r.title }}</td>
              <td>{{ categorias.find((c) => c.categoryId === r.categoryId)?.name || '—' }}</td>
              <td>{{ marcas.find((m) => m.brandId === r.brandId)?.name || '—' }}</td>
              <td>${{ r.price }}</td>
              <td>
                <strong>{{ r.availableQuantity || 0 }}</strong>
                <span v-if="r.reservedQuantity > 0" class="text-muted"> ({{ r.reservedQuantity }} reservado)</span>
              </td>
              <td>
                <span v-if="!r.inStock" class="badge badge-danger">Sin stock</span>
                <span v-else-if="r.lowStock" class="badge badge-warning">Bajo stock</span>
                <span v-else class="badge badge-success">Disponible</span>
              </td>
              <td class="actions">
                <button @click="openInventarioModal(r)" class="btn-icon" title="Agregar inventario">➕</button>
                <button @click="editRepuesto(r)" class="btn-icon" title="Editar">✏️</button>
                <button @click="deleteRepuesto(r)" class="btn-icon" title="Eliminar">🗑️</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Modal: Agregar inventario -->
      <div v-if="showInventarioModal" class="modal-overlay" @click.self="closeInventarioModal">
        <div class="modal-card">
          <h3>Agregar inventario</h3>
          <p v-if="inventarioRepuesto" class="modal-part">{{ inventarioRepuesto.title }} ({{ inventarioRepuesto.partNumber }})</p>
          <p class="modal-stock">Stock actual: <strong>{{ inventarioRepuesto?.stockQuantity ?? 0 }}</strong></p>
          <div class="form-group">
            <label>Cantidad a agregar</label>
            <input v-model.number="inventarioCantidad" type="number" min="1" />
          </div>
          <div class="modal-actions">
            <button type="button" class="btn btn-secondary" @click="closeInventarioModal">Cancelar</button>
            <button type="button" class="btn btn-primary" :disabled="inventarioSaving || inventarioCantidad < 1" @click="submitInventario">
              {{ inventarioSaving ? 'Guardando…' : 'Registrar alta' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- TAB: Categorías -->
    <div v-show="activeTab === 'categorias'" class="tab-content">
      <div class="section-header">
        <h2>Categorías</h2>
        <button class="btn btn-primary" @click="showCategoriaForm = !showCategoriaForm">
          {{ showCategoriaForm ? 'Cancelar' : '+ Nueva categoría' }}
        </button>
      </div>

      <div v-if="showCategoriaForm" class="form-card">
        <h3>Nueva categoría</h3>
        <form @submit.prevent="submitCategoria" class="form-grid">
          <div class="form-group full-width">
            <label>Nombre *</label>
            <input v-model="categoriaForm.name" required placeholder="Motor" />
          </div>
          <div class="form-group full-width">
            <label>Categoría padre (opcional)</label>
            <select v-model="categoriaForm.parentId">
              <option :value="null">Ninguna (categoría raíz)</option>
              <option v-for="c in categorias" :key="c.categoryId" :value="c.categoryId">{{ c.name }}</option>
            </select>
          </div>
          <div class="form-group full-width">
            <label>Imagen (opcional)</label>
            <ImageUpload v-model="categoriaForm.imageData" />
          </div>
          <div class="form-actions full-width">
            <button type="submit" class="btn btn-primary">Crear categoría</button>
          </div>
        </form>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>Padre</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in categorias" :key="c.categoryId">
              <td>{{ c.categoryId }}</td>
              <td>{{ c.name }}</td>
              <td>{{ c.parentId ? categorias.find((p) => p.categoryId === c.parentId)?.name || c.parentId : '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- TAB: Marcas -->
    <div v-show="activeTab === 'marcas'" class="tab-content">
      <div class="section-header">
        <h2>Marcas</h2>
        <button class="btn btn-primary" @click="showMarcaForm = !showMarcaForm">
          {{ showMarcaForm ? 'Cancelar' : '+ Nueva marca' }}
        </button>
      </div>

      <div v-if="showMarcaForm" class="form-card">
        <h3>Nueva marca</h3>
        <form @submit.prevent="submitMarca" class="form-grid">
          <div class="form-group full-width">
            <label>Nombre *</label>
            <input v-model="marcaForm.name" required placeholder="Bosch" />
          </div>
          <div class="form-group full-width">
            <label>Imagen (opcional)</label>
            <ImageUpload v-model="marcaForm.imageData" />
          </div>
          <div class="form-actions full-width">
            <button type="submit" class="btn btn-primary">Crear marca</button>
          </div>
        </form>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in marcas" :key="m.brandId">
              <td>{{ m.brandId }}</td>
              <td>{{ m.name }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.catalogo-page {
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
  color: #64748b;
  font-weight: 500;
  cursor: pointer;
  margin-bottom: -2px;
  transition: color 0.15s, border-color 0.15s;
}
.tabs button:hover {
  color: #0f172a;
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
  gap: 0.75rem;
}
.section-header h2 {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
  color: #0f172a;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.hidden {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}
.import-result {
  padding: 12px 16px;
  margin-bottom: 1rem;
  background: #f0fdf4;
  border: 1px solid #86efac;
  border-radius: 8px;
  font-size: 14px;
}
.import-detail {
  margin: 8px 0 0;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
  font-size: 12px;
  max-height: 120px;
  overflow: auto;
  white-space: pre-wrap;
}
.search-card {
  padding: 1rem 1.25rem;
  margin-bottom: 1rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}
.search-card h3 {
  font-size: 0.9375rem;
  font-weight: 600;
  margin: 0 0 0.25rem;
  color: #0f172a;
}
.search-hint {
  font-size: 0.8125rem;
  color: #64748b;
  margin: 0 0 0.75rem;
}
.search-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.search-input {
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 14px;
  min-width: 140px;
}
.btn-sm {
  padding: 6px 12px;
  font-size: 0.8125rem;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-card {
  background: var(--card-bg);
  border-radius: 12px;
  padding: 1.5rem;
  min-width: 320px;
  max-width: 90vw;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}
.modal-card h3 {
  margin: 0 0 0.5rem;
  font-size: 1.125rem;
  color: #0f172a;
}
.modal-part {
  margin: 0 0 0.25rem;
  font-size: 0.9375rem;
  color: #475569;
}
.modal-stock {
  margin: 0 0 1rem;
  font-size: 0.9375rem;
  color: #64748b;
}
.modal-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 1rem;
}
.form-card {
  background: var(--card-bg);
  border-radius: var(--radius);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  border: 1px solid #e2e8f0;
}
.form-card h3 {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 1rem;
  color: #0f172a;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.form-group.full-width {
  grid-column: 1 / -1;
}
.form-group label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}
.form-group input,
.form-group select,
.form-group textarea {
  padding: 0.6rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: var(--radius-sm);
  font-size: 1rem;
}
.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--sidebar-accent);
  box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.2);
}
.form-actions {
  display: flex;
  gap: 0.75rem;
}
.alert {
  padding: 1rem;
  border-radius: var(--radius-sm);
  margin-bottom: 1rem;
}
.alert-error {
  background: #fee2e2;
  color: #b91c1c;
}
.loading {
  padding: 2rem;
  color: #64748b;
}
.table-wrap {
  overflow-x: auto;
  background: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: var(--card-shadow);
  border: 1px solid #e2e8f0;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9375rem;
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
.data-table tbody tr:hover {
  background: #f8fafc;
}
.data-table code {
  background: #f1f5f9;
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  font-size: 0.875rem;
  color: #0f172a;
}
.btn {
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: var(--radius-sm);
  cursor: pointer;
  border: none;
  transition: background 0.15s, color 0.15s;
}
.btn-primary {
  background: var(--sidebar-bg);
  color: #fff;
}
.btn-primary:hover {
  background: var(--sidebar-hover);
}
.btn-secondary {
  background: #f3f4f6;
  color: #374151;
  margin-right: 8px;
}
.btn-secondary:hover {
  background: #e5e7eb;
}
.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 4px 8px;
  transition: transform 0.2s;
}
.btn-icon:hover {
  transform: scale(1.2);
}
.actions {
  display: flex;
  gap: 4px;
}
.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.badge-success {
  background: #d1fae5;
  color: #065f46;
}
.badge-warning {
  background: #fef3c7;
  color: #92400e;
}
.badge-danger {
  background: #fee2e2;
  color: #991b1b;
}
.text-muted {
  color: #6b7280;
  font-size: 0.85rem;
}
.form-group small {
  display: block;
  margin-top: 4px;
  font-size: 0.8rem;
  color: #6b7280;
}
</style>
