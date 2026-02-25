<template>
  <div class="tienda-page">
    <header class="page-header">
      <h1>🛒 Tienda de Repuestos</h1>
      <p class="page-subtitle">Encuentra los mejores repuestos para tu vehículo</p>
    </header>

    <!-- Filtros -->
    <div class="filters-section">
      <div class="filter-group">
        <label>Categoría:</label>
        <select v-model="selectedCategory">
          <option :value="null">Todas las categorías</option>
          <option v-for="cat in categorias" :key="cat.categoryId" :value="cat.categoryId">
            {{ cat.name }}
          </option>
        </select>
      </div>

      <div class="filter-group">
        <label>Marca:</label>
        <select v-model="selectedBrand">
          <option :value="null">Todas las marcas</option>
          <option v-for="brand in marcas" :key="brand.brandId" :value="brand.brandId">
            {{ brand.name }}
          </option>
        </select>
      </div>

      <div class="filter-group">
        <label>Buscar:</label>
        <input v-model="searchQuery" type="text" placeholder="Nombre o número de parte..." />
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading">Cargando repuestos...</div>

    <!-- Error -->
    <div v-if="error" class="error-message">{{ error }}</div>

    <!-- Grid de productos -->
    <div v-if="!loading && !error" class="products-grid">
      <div v-for="part in filteredParts" :key="part.partId" class="product-card">
        <router-link :to="`/producto/${part.partId}`" class="product-link">
          <div class="product-image">
            <img v-if="part.hasImage" :src="`http://localhost:8080/api/images/part/${part.partId}`" :alt="part.title" />
            <div v-else class="no-image">📦</div>
          </div>
          
          <div class="product-info">
            <h3 class="product-title">{{ part.title }}</h3>
            <p class="product-number">No. Parte: {{ part.partNumber }}</p>
            <p class="product-description">{{ part.description || 'Sin descripción' }}</p>
            
            <!-- Badge de inventario -->
            <div class="product-stock">
              <span v-if="!part.inStock" class="stock-badge out">Agotado</span>
              <span v-else-if="part.lowStock" class="stock-badge low">Bajo stock</span>
              <span v-else class="stock-badge in">Disponible</span>
            </div>
            
            <div class="product-footer">
              <span class="product-price">${{ part.price.toFixed(2) }}</span>
            </div>
          </div>
        </router-link>
        
        <div class="product-actions">
          <button 
            class="btn btn-primary" 
            @click.stop="addToCart(part)"
            :disabled="!part.inStock"
          >
            <span v-if="!part.inStock">❌ Agotado</span>
            <span v-else>🛒 Agregar</span>
          </button>
        </div>
      </div>

      <div v-if="filteredParts.length === 0" class="no-results">
        No se encontraron repuestos con los filtros seleccionados.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { useToast } from '../composables/useToast'
import { useCart } from '../composables/useCart'
import { listCategorias, listMarcas, listRepuestos } from '../api/catalogo'

const router = useRouter()
const { isLoggedIn } = useAuth()
const { success, info } = useToast()
const { addToCart: addToCartComposable } = useCart()

const categorias = ref([])
const marcas = ref([])
const repuestos = ref([])
const loading = ref(true)
const error = ref('')

const selectedCategory = ref(null)
const selectedBrand = ref(null)
const searchQuery = ref('')

onMounted(async () => {
  try {
    const [c, m, r] = await Promise.all([
      listCategorias(),
      listMarcas(),
      listRepuestos()
    ])
    categorias.value = c
    marcas.value = m
    repuestos.value = r.filter(p => p.active === 1)
  } catch (e) {
    error.value = e.message || 'Error al cargar datos'
  } finally {
    loading.value = false
  }
})

const filteredParts = computed(() => {
  let filtered = repuestos.value

  if (selectedCategory.value) {
    filtered = filtered.filter(p => p.categoryId === selectedCategory.value)
  }

  if (selectedBrand.value) {
    filtered = filtered.filter(p => p.brandId === selectedBrand.value)
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(p => 
      p.title.toLowerCase().includes(query) ||
      p.partNumber.toLowerCase().includes(query) ||
      (p.description && p.description.toLowerCase().includes(query))
    )
  }

  return filtered
})

function addToCart(part) {
  // Verificar si el usuario está autenticado
  if (!isLoggedIn.value) {
    info('Debes iniciar sesión para agregar productos al carrito')
    router.push({ name: 'Login', query: { redirect: `/producto/${part.partId}` } })
    return
  }

  addToCartComposable(part, 1)
  success(`"${part.title}" agregado al carrito`)
}
</script>

<style scoped>
.tienda-page {
  max-width: 1400px;
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

.filters-section {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
  flex-wrap: wrap;
}

.filter-group {
  flex: 1;
  min-width: 200px;
}

.filter-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 8px;
  color: #374151;
}

.filter-group select,
.filter-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.product-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}

.product-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.product-link {
  text-decoration: none;
  color: inherit;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.product-image {
  width: 100%;
  height: 200px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  font-size: 64px;
  color: #9ca3af;
}

.product-info {
  padding: 16px;
}

.product-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.product-number {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 8px;
}

.product-description {
  font-size: 14px;
  color: #4b5563;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-stock {
  margin-bottom: 12px;
}

.stock-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.stock-badge.in {
  background: #d1fae5;
  color: #065f46;
}

.stock-badge.low {
  background: #fef3c7;
  color: #92400e;
}

.stock-badge.out {
  background: #fee2e2;
  color: #991b1b;
}

.product-footer {
  margin-top: auto;
}

.product-price {
  font-size: 24px;
  font-weight: 700;
  color: #10b981;
}

.product-actions {
  padding: 0 16px 16px;
}

.btn {
  padding: 10px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}

.loading,
.error-message,
.no-results {
  text-align: center;
  padding: 40px;
  color: #6b7280;
}

.error-message {
  color: #ef4444;
}
</style>
