<template>
  <div class="carrito-page">
    <header class="page-header">
      <h1>🛒 Mi Carrito</h1>
    </header>

    <div v-if="cartItems.length === 0" class="empty-cart">
      <p>Tu carrito está vacío</p>
      <router-link to="/" class="btn btn-primary">Ir a la tienda</router-link>
    </div>

    <div v-else class="cart-content">
      <div class="cart-items">
        <div v-for="item in cartItems" :key="item.partId" class="cart-item-wrapper">
          <div class="cart-item">
            <div class="item-image">
              <img v-if="item.hasImage" :src="`http://localhost:8080/api/images/part/${item.partId}`" :alt="item.title" />
              <div v-else class="no-image">📦</div>
            </div>

            <div class="item-info">
              <h3>{{ item.title }}</h3>
              <p class="item-number">No. Parte: {{ item.partNumber }}</p>
              <p class="item-price">${{ item.price.toFixed(2) }} c/u</p>
            </div>

            <div class="item-quantity">
              <button @click="updateQuantity(item.partId, item.qty - 1)" class="qty-btn" :disabled="item.qty <= 1">−</button>
              <input 
                type="number" 
                :value="item.qty" 
                @input="updateQuantity(item.partId, parseInt($event.target.value))"
                min="1"
              />
              <button @click="updateQuantity(item.partId, item.qty + 1)" class="qty-btn">+</button>
            </div>

            <div class="item-total">
              <span class="total-label">Total:</span>
              <span class="total-price">${{ (item.price * item.qty).toFixed(2) }}</span>
            </div>

            <button @click="removeFromCart(item.partId)" class="btn-remove" title="Eliminar">
              🗑️
            </button>
          </div>
          
          <!-- Alertas de stock -->
          <div v-if="getWarningForItem(item.partId)" class="stock-alert" :class="getWarningForItem(item.partId).type">
            <span class="alert-icon">
              {{ getWarningForItem(item.partId).type === 'out' ? '❌' : getWarningForItem(item.partId).type === 'insufficient' ? '⚠️' : 'ℹ️' }}
            </span>
            <span class="alert-message">{{ getWarningForItem(item.partId).message }}</span>
            <button 
              v-if="getWarningForItem(item.partId).type === 'insufficient'"
              @click="adjustQuantity(item.partId, getWarningForItem(item.partId).maxQty)"
              class="btn-adjust"
            >
              Ajustar a {{ getWarningForItem(item.partId).maxQty }}
            </button>
          </div>
        </div>
      </div>

      <div class="cart-summary">
        <h2>Resumen del pedido</h2>
        
        <div class="summary-row">
          <span>Subtotal ({{ cartCount }} artículos):</span>
          <span>${{ cartTotal.toFixed(2) }}</span>
        </div>

        <div class="summary-row">
          <span>Envío:</span>
          <span>Gratis</span>
        </div>

        <div class="summary-row total-row">
          <span>Total:</span>
          <span>${{ cartTotal.toFixed(2) }}</span>
        </div>

        <!-- Alertas generales -->
        <div v-if="hasStockIssues" class="checkout-warning">
          ⚠️ Algunos productos tienen problemas de stock. Ajusta las cantidades antes de continuar.
        </div>

        <button 
          v-if="isLoggedIn" 
          @click="proceedToCheckout" 
          class="btn btn-primary btn-block"
          :disabled="processing || hasStockIssues"
        >
          {{ processing ? 'Procesando...' : 'Proceder al pago' }}
        </button>

        <div v-else class="login-prompt">
          <p>Debes iniciar sesión para continuar</p>
          <router-link to="/login" class="btn btn-primary btn-block">Iniciar sesión</router-link>
        </div>

        <router-link to="/" class="btn btn-secondary btn-block">
          ← Continuar comprando
        </router-link>

        <button @click="clearCart" class="btn btn-secondary btn-block">
          Vaciar carrito
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { useCart } from '../composables/useCart'
import { useToast } from '../composables/useToast'
import { getRepuesto } from '../api/catalogo'

const router = useRouter()
const { user, isLoggedIn } = useAuth()
const { cartItems, cartTotal, cartCount, removeFromCart, updateQuantity, clearCart } = useCart()
const { success, error: showError, warning } = useToast()

const processing = ref(false)
const stockWarnings = ref([])

// Verificar stock de todos los productos en el carrito
async function checkStockAvailability() {
  stockWarnings.value = []
  
  for (const item of cartItems.value) {
    try {
      const product = await getRepuesto(item.partId)
      
      if (!product.inStock) {
        stockWarnings.value.push({
          partId: item.partId,
          title: item.title,
          message: 'Producto sin stock',
          type: 'out'
        })
      } else if (product.availableQuantity < item.qty) {
        stockWarnings.value.push({
          partId: item.partId,
          title: item.title,
          message: `Solo hay ${product.availableQuantity} disponibles`,
          type: 'insufficient',
          maxQty: product.availableQuantity
        })
      } else if (product.lowStock) {
        stockWarnings.value.push({
          partId: item.partId,
          title: item.title,
          message: `Bajo inventario (${product.availableQuantity} disponibles)`,
          type: 'low'
        })
      }
    } catch (e) {
      console.error('Error verificando stock:', e)
    }
  }
}

const hasStockIssues = computed(() => {
  return stockWarnings.value.some(w => w.type === 'out' || w.type === 'insufficient')
})

async function proceedToCheckout() {
  if (!user.value) {
    showError('Debes iniciar sesión para continuar')
    router.push('/login')
    return
  }
  processing.value = true
  try {
    await checkStockAvailability()
    if (hasStockIssues.value) {
      showError('Algunos productos no tienen stock suficiente. Por favor, ajusta las cantidades.')
      return
    }
    router.push('/checkout')
  } finally {
    processing.value = false
  }
}

function getWarningForItem(partId) {
  return stockWarnings.value.find(w => w.partId === partId)
}

function adjustQuantity(partId, maxQty) {
  updateQuantity(partId, maxQty)
  checkStockAvailability()
}
</script>

<style scoped>
.carrito-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 24px;
  min-height: 60vh;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.empty-cart {
  text-align: center;
  padding: 80px 24px;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
  border-radius: 16px;
  border: 1px dashed #e2e8f0;
}

.empty-cart p {
  font-size: 17px;
  color: #64748b;
  margin-bottom: 24px;
}

.cart-content {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 32px;
  align-items: start;
}

@media (max-width: 968px) {
  .cart-content {
    grid-template-columns: 1fr;
  }
}

.cart-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cart-item-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cart-item {
  display: grid;
  grid-template-columns: 100px 1fr auto auto auto;
  gap: 20px;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  transition: box-shadow 0.2s, border-color 0.2s;
}

.cart-item:hover {
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}

.stock-alert {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
}

.stock-alert.out {
  background: #fef2f2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.stock-alert.insufficient {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
}

.stock-alert.low {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
}

.alert-icon {
  font-size: 18px;
}

.alert-message {
  flex: 1;
  font-weight: 500;
}

.btn-adjust {
  padding: 8px 14px;
  background: #f59e0b;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.1s;
}

.btn-adjust:hover {
  background: #d97706;
  transform: translateY(-1px);
}

.item-image {
  width: 100px;
  height: 100px;
  background: #f1f5f9;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  font-size: 36px;
  color: #94a3b8;
}

.item-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 6px;
  line-height: 1.3;
}

.item-number {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}

.item-price {
  font-size: 14px;
  color: #475569;
  font-weight: 500;
}

.item-quantity {
  display: flex;
  align-items: center;
  gap: 10px;
}

.qty-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #e2e8f0;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  font-size: 18px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #475569;
  transition: background 0.2s, border-color 0.2s;
}

.qty-btn:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.qty-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.item-quantity input {
  width: 56px;
  padding: 8px 4px;
  text-align: center;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.item-total {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  min-width: 80px;
}

.total-label {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 2px;
}

.total-price {
  font-size: 18px;
  font-weight: 700;
  color: #0d9488;
}

.btn-remove {
  width: 44px;
  height: 44px;
  border: none;
  background: #fef2f2;
  border-radius: 10px;
  cursor: pointer;
  font-size: 18px;
  transition: background 0.2s, transform 0.1s;
}

.btn-remove:hover {
  background: #fee2e2;
  transform: scale(1.05);
}

.cart-summary {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 28px;
  height: fit-content;
  position: sticky;
  top: 24px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}

.cart-summary h2 {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f1f5f9;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 14px 0;
  font-size: 14px;
  color: #475569;
  border-bottom: 1px solid #f1f5f9;
}

.total-row {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  border-bottom: none;
  padding-top: 16px;
  margin-top: 4px;
}

.btn {
  padding: 14px 20px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #0d9488;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #0f766e;
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.btn-secondary {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.btn-secondary:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.btn-block {
  width: 100%;
  margin-top: 10px;
}

.login-prompt {
  margin-top: 20px;
  padding: 20px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 12px;
  text-align: center;
}

.login-prompt p {
  margin-bottom: 14px;
  color: #92400e;
  font-weight: 500;
}

.checkout-warning {
  margin: 16px 0;
  padding: 14px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
  color: #92400e;
  font-size: 14px;
  font-weight: 500;
  text-align: center;
}
</style>
