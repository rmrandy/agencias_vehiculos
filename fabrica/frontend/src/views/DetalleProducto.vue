<template>
  <div class="detalle-producto-page">
    <div v-if="loading" class="loading">Cargando producto...</div>

    <div v-else-if="error" class="error-message">{{ error }}</div>

    <div v-else-if="producto" class="producto-detalle">
      <!-- Breadcrumb -->
      <div class="breadcrumb">
        <router-link to="/">Tienda</router-link>
        <span class="separator">›</span>
        <span>{{ producto.title }}</span>
      </div>

      <div class="producto-content">
        <!-- Galería de imágenes -->
        <div class="producto-gallery">
          <div class="main-image">
            <img 
              v-if="producto.hasImage" 
              :src="`http://localhost:8080/api/images/part/${producto.partId}`" 
              :alt="producto.title"
            />
            <div v-else class="no-image">📦</div>
          </div>
          
          <!-- Thumbnails (por ahora solo una imagen, pero preparado para múltiples) -->
          <div class="thumbnails">
            <div 
              v-if="producto.hasImage" 
              class="thumbnail active"
            >
              <img :src="`http://localhost:8080/api/images/part/${producto.partId}`" :alt="producto.title" />
            </div>
          </div>
        </div>

        <!-- Información del producto -->
        <div class="producto-info">
          <h1 class="producto-title">{{ producto.title }}</h1>
          
          <div class="producto-meta">
            <span class="part-number">No. Parte: <strong>{{ producto.partNumber }}</strong></span>
            <span v-if="categoria" class="category">Categoría: <strong>{{ categoria.name }}</strong></span>
            <span v-if="marca" class="brand">Marca: <strong>{{ marca.name }}</strong></span>
          </div>

          <div class="producto-price">
            <span class="price-label">Precio:</span>
            <span class="price-amount">${{ producto.price.toFixed(2) }}</span>
          </div>

          <!-- Estado de inventario -->
          <div class="producto-stock">
            <div v-if="!producto.inStock" class="stock-badge out-of-stock">
              ❌ Fuera de Stock
            </div>
            <div v-else-if="producto.lowStock" class="stock-badge low-stock">
              ⚠️ Bajo inventario ({{ producto.availableQuantity }} disponibles)
            </div>
            <div v-else class="stock-badge in-stock">
              ✅ En Stock ({{ producto.availableQuantity }} disponibles)
            </div>
          </div>

          <div class="producto-description">
            <h3>Descripción</h3>
            <p>{{ producto.description || 'Sin descripción disponible' }}</p>
          </div>

          <div v-if="producto.weightLb" class="producto-specs">
            <h3>Especificaciones</h3>
            <ul>
              <li><strong>Peso:</strong> {{ producto.weightLb }} lb</li>
            </ul>
          </div>

          <!-- Acciones -->
          <div class="producto-actions">
            <div v-if="producto.inStock" class="quantity-selector">
              <label>Cantidad:</label>
              <div class="qty-controls">
                <button @click="qty = Math.max(1, qty - 1)" class="qty-btn">−</button>
                <input 
                  v-model.number="qty" 
                  type="number" 
                  min="1" 
                  :max="producto.availableQuantity"
                  @input="qty = Math.min(qty, producto.availableQuantity)"
                />
                <button 
                  @click="qty = Math.min(qty + 1, producto.availableQuantity)" 
                  class="qty-btn"
                  :disabled="qty >= producto.availableQuantity"
                >+</button>
              </div>
              <span class="qty-hint">Máximo: {{ producto.availableQuantity }}</span>
            </div>

            <button 
              @click="addToCart" 
              class="btn btn-primary btn-large"
              :disabled="!producto.inStock"
            >
              <span v-if="!producto.inStock">❌ Fuera de Stock</span>
              <span v-else-if="!isLoggedIn">🔒 Iniciar sesión para comprar</span>
              <span v-else>🛒 Agregar al carrito</span>
            </button>

            <router-link to="/" class="btn btn-secondary btn-large">
              ← Volver a la tienda
            </router-link>
          </div>
        </div>
      </div>

      <!-- Comentarios y valoraciones -->
      <section class="comentarios-section">
        <h2 class="section-title">Comentarios y valoraciones</h2>
        <div v-if="comentariosPromedio != null" class="promedio-box">
          <span class="promedio-label">Valoración media:</span>
          <span class="promedio-stars">
            <span v-for="n in 5" :key="n" class="star" :class="{ filled: n <= Math.round(comentariosPromedio) }">★</span>
          </span>
          <span class="promedio-num">({{ comentariosPromedio.toFixed(1) }})</span>
        </div>
        <div v-else-if="!comentariosLoading && comentarios.length === 0" class="sin-comentarios">
          Aún no hay comentarios. ¡Sé el primero en valorar este producto!
        </div>

        <div v-if="isLoggedIn" class="nuevo-comentario">
          <h3>Deja tu comentario</h3>
          <div class="rating-input">
            <label>Puntuación (1-5 estrellas):</label>
            <div class="stars-select">
              <button
                v-for="n in 5"
                :key="n"
                type="button"
                class="star-btn"
                :class="{ active: n <= (nuevoRating || 0) }"
                :title="`${n} estrellas`"
                @click="nuevoRating = n"
              >★</button>
            </div>
          </div>
          <textarea
            v-model="nuevoComentarioBody"
            placeholder="Escribe tu comentario..."
            rows="3"
            class="comentario-textarea"
          />
          <button
            type="button"
            class="btn btn-primary"
            :disabled="!nuevoComentarioBody.trim()"
            @click="enviarComentario"
          >
            Publicar comentario
          </button>
        </div>
        <p v-else class="login-hint">
          <router-link :to="{ name: 'Login', query: { redirect: route.fullPath } }">Inicia sesión</router-link>
          para dejar un comentario y valorar el producto.
        </p>

        <div v-if="comentariosLoading" class="comentarios-loading">Cargando comentarios...</div>
        <div v-else class="comentarios-list">
          <CommentItem
            v-for="c in comentarios"
            :key="c.reviewId"
            :comment="c"
            :part-id="producto.partId"
            :current-user-id="user?.userId ?? null"
            :is-logged-in="isLoggedIn"
            @reply="loadComentarios"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { useCart } from '../composables/useCart'
import { useToast } from '../composables/useToast'
import { apiFetch } from '../api/config'
import { getComentarios, createComentario } from '../api/comentarios'
import CommentItem from '../components/CommentItem.vue'

const route = useRoute()
const router = useRouter()
const { user, isLoggedIn } = useAuth()
const { addToCart: addToCartComposable } = useCart()
const { success, info, error: showError } = useToast()

const producto = ref(null)
const categoria = ref(null)
const marca = ref(null)
const loading = ref(true)
const error = ref('')
const qty = ref(1)

const comentarios = ref([])
const comentariosPromedio = ref(null)
const comentariosLoading = ref(false)
const nuevoRating = ref(null)
const nuevoComentarioBody = ref('')

async function loadComentarios() {
  if (!producto.value?.partId) return
  comentariosLoading.value = true
  try {
    const data = await getComentarios(producto.value.partId)
    comentarios.value = data.comentarios || []
    comentariosPromedio.value = data.promedio ?? null
  } catch (e) {
    comentarios.value = []
    comentariosPromedio.value = null
  } finally {
    comentariosLoading.value = false
  }
}

async function enviarComentario() {
  const body = nuevoComentarioBody.value?.trim()
  if (!body || !isLoggedIn.value || !user.value?.userId) {
    info('Escribe un comentario para publicar')
    return
  }
  try {
    await createComentario(producto.value.partId, {
      userId: user.value.userId,
      body,
      rating: nuevoRating.value ?? undefined,
    })
    success('Comentario publicado')
    nuevoComentarioBody.value = ''
    nuevoRating.value = null
    await loadComentarios()
  } catch (e) {
    showError(e.message || 'Error al publicar el comentario')
  }
}

onMounted(async () => {
  const partId = route.params.id
  try {
    producto.value = await apiFetch(`/api/repuestos/${partId}`)
    
    if (producto.value.categoryId) {
      categoria.value = await apiFetch(`/api/categorias/${producto.value.categoryId}`)
    }
    if (producto.value.brandId) {
      marca.value = await apiFetch(`/api/marcas/${producto.value.brandId}`)
    }

    await loadComentarios()
  } catch (e) {
    error.value = e.message || 'Error al cargar el producto'
    console.error('Error cargando producto:', e)
  } finally {
    loading.value = false
  }
})

function addToCart() {
  // Verificar si el usuario está autenticado
  if (!isLoggedIn.value) {
    info('Debes iniciar sesión para agregar productos al carrito')
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }

  if (producto.value) {
    addToCartComposable(producto.value, qty.value)
    success(`${qty.value}x "${producto.value.title}" agregado al carrito`)
    qty.value = 1
  }
}
</script>

<style scoped>
.detalle-producto-page {
  max-width: 1400px;
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

.breadcrumb {
  margin-bottom: 20px;
  font-size: 14px;
  color: #6b7280;
}

.breadcrumb a {
  color: #3b82f6;
  text-decoration: none;
}

.breadcrumb a:hover {
  text-decoration: underline;
}

.separator {
  margin: 0 8px;
}

.producto-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
}

@media (max-width: 968px) {
  .producto-content {
    grid-template-columns: 1fr;
  }
}

.producto-gallery {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.main-image {
  width: 100%;
  aspect-ratio: 1;
  background: #f3f4f6;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  font-size: 120px;
  color: #9ca3af;
}

.thumbnails {
  display: flex;
  gap: 12px;
}

.thumbnail {
  width: 80px;
  height: 80px;
  background: #f3f4f6;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.thumbnail:hover {
  border-color: #3b82f6;
}

.thumbnail.active {
  border-color: #3b82f6;
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.producto-info {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.producto-title {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.producto-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
  color: #6b7280;
}

.producto-meta strong {
  color: #1f2937;
}

.producto-price {
  padding: 20px;
  background: #f9fafb;
  border-radius: 8px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.price-label {
  font-size: 16px;
  color: #6b7280;
}

.price-amount {
  font-size: 40px;
  font-weight: 700;
  color: #10b981;
}

.producto-stock {
  margin: 16px 0;
}

.stock-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
}

.stock-badge.in-stock {
  background: #d1fae5;
  color: #065f46;
}

.stock-badge.low-stock {
  background: #fef3c7;
  color: #92400e;
}

.stock-badge.out-of-stock {
  background: #fee2e2;
  color: #991b1b;
}

.producto-description h3,
.producto-specs h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
}

.producto-description p {
  font-size: 16px;
  line-height: 1.6;
  color: #4b5563;
}

.producto-specs ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.producto-specs li {
  padding: 8px 0;
  font-size: 16px;
  color: #4b5563;
  border-bottom: 1px solid #e5e7eb;
}

.producto-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 20px;
  border-top: 2px solid #e5e7eb;
}

.quantity-selector {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quantity-selector label {
  font-weight: 600;
  color: #374151;
  font-size: 15px;
}

.qty-hint {
  font-size: 13px;
  color: #6b7280;
  font-style: italic;
}

.qty-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qty-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.qty-btn:hover {
  background: #f3f4f6;
  border-color: #3b82f6;
}

.qty-controls input {
  width: 80px;
  padding: 10px;
  text-align: center;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
}

.btn {
  padding: 14px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-large {
  padding: 16px 32px;
  font-size: 18px;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.qty-btn:hover:not(:disabled) {
  background: #e5e7eb;
}

.qty-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Comentarios y valoraciones */
.comentarios-section {
  margin-top: 48px;
  padding-top: 32px;
  border-top: 2px solid #e5e7eb;
  grid-column: 1 / -1;
}
.section-title {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 20px;
}
.promedio-box {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}
.promedio-label {
  font-size: 15px;
  color: #6b7280;
}
.promedio-stars .star {
  color: #d1d5db;
  font-size: 20px;
}
.promedio-stars .star.filled {
  color: #f59e0b;
}
.promedio-num {
  font-size: 15px;
  color: #6b7280;
}
.sin-comentarios {
  color: #6b7280;
  font-size: 15px;
  margin-bottom: 20px;
}
.nuevo-comentario {
  margin-bottom: 28px;
  padding: 20px;
  background: #f9fafb;
  border-radius: 12px;
}
.nuevo-comentario h3 {
  margin: 0 0 16px;
  font-size: 16px;
  color: #374151;
}
.rating-input {
  margin-bottom: 12px;
}
.rating-input label {
  display: block;
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}
.stars-select {
  display: flex;
  gap: 4px;
}
.star-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #d1d5db;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s;
}
.star-btn:hover,
.star-btn.active {
  color: #f59e0b;
}
.comentario-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  resize: vertical;
  margin-bottom: 12px;
  box-sizing: border-box;
}
.login-hint {
  margin-bottom: 24px;
  font-size: 15px;
  color: #6b7280;
}
.login-hint a {
  color: #3b82f6;
  text-decoration: none;
}
.login-hint a:hover {
  text-decoration: underline;
}
.comentarios-loading {
  color: #6b7280;
  padding: 20px 0;
}
.comentarios-list {
  margin-top: 16px;
}
</style>
