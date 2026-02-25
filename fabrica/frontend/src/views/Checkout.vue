<template>
  <div class="checkout-page">
    <header class="page-header">
      <h1>💳 Finalizar compra</h1>
    </header>

    <div v-if="cartItems.length === 0" class="empty-checkout">
      <p>No hay productos en el carrito</p>
      <router-link to="/" class="btn btn-primary">Ir a la tienda</router-link>
    </div>

    <div v-else class="checkout-content">
      <div class="checkout-main">
        <!-- Resumen de productos -->
        <section class="order-summary-section">
          <h2>Resumen del pedido</h2>
          <div class="order-items">
            <div v-for="item in cartItems" :key="item.partId" class="order-item">
              <div class="item-thumb">
                <img v-if="item.hasImage" :src="`http://localhost:8080/api/images/part/${item.partId}`" :alt="item.title" />
                <span v-else class="no-img">📦</span>
              </div>
              <div class="item-details">
                <span class="item-title">{{ item.title }}</span>
                <span class="item-meta">{{ item.partNumber }} · {{ item.qty }} × ${{ item.price.toFixed(2) }}</span>
              </div>
              <span class="item-line-total">${{ (item.price * item.qty).toFixed(2) }}</span>
            </div>
          </div>
        </section>

        <!-- Formulario de pago -->
        <section class="payment-section">
          <h2>Datos de pago</h2>
          <form @submit.prevent="submitPayment" class="payment-form">
            <div class="form-group">
              <label for="cardNumber">Número de tarjeta</label>
              <input
                id="cardNumber"
                v-model="cardDisplay"
                type="text"
                inputmode="numeric"
                maxlength="19"
                placeholder="1234 5678 9012 3456"
                autocomplete="cc-number"
                :class="{ error: errors.cardNumber }"
                @input="formatCardNumber"
              />
              <span v-if="errors.cardNumber" class="field-error">{{ errors.cardNumber }}</span>
              <span class="hint">Entre 13 y 19 dígitos</span>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label for="expiryMonth">Mes</label>
                <select id="expiryMonth" v-model.number="expiryMonth" :class="{ error: errors.expiry }">
                  <option :value="null">Mes</option>
                  <option v-for="m in 12" :key="m" :value="m">{{ String(m).padStart(2, '0') }}</option>
                </select>
              </div>
              <div class="form-group">
                <label for="expiryYear">Año</label>
                <select id="expiryYear" v-model.number="expiryYear" :class="{ error: errors.expiry }">
                  <option :value="null">Año</option>
                  <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
                </select>
              </div>
            </div>
            <span v-if="errors.expiry" class="field-error">{{ errors.expiry }}</span>

            <div class="form-group">
              <label for="cardholder">Titular de la tarjeta (opcional)</label>
              <input id="cardholder" v-model="cardholder" type="text" placeholder="Nombre como en la tarjeta" />
            </div>

            <button type="submit" class="btn btn-primary btn-block btn-pay" :disabled="processing">
              {{ processing ? 'Procesando...' : `Pagar $${cartTotal.toFixed(2)}` }}
            </button>
          </form>
        </section>
      </div>

      <aside class="checkout-sidebar">
        <div class="sidebar-box">
          <h3>Total a pagar</h3>
          <div class="sidebar-row">
            <span>Subtotal ({{ cartCount }} artículos)</span>
            <span>${{ cartTotal.toFixed(2) }}</span>
          </div>
          <div class="sidebar-row">
            <span>Envío</span>
            <span>Gratis</span>
          </div>
          <div class="sidebar-row total">
            <span>Total</span>
            <span>${{ cartTotal.toFixed(2) }}</span>
          </div>
        </div>
        <p class="secure-note">🔒 No guardamos los datos de tu tarjeta.</p>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { useCart } from '../composables/useCart'
import { useToast } from '../composables/useToast'
import { createOrder } from '../api/pedidos'

const router = useRouter()
const { user } = useAuth()
const { cartItems, cartTotal, cartCount, clearCart } = useCart()
const { success, error: showError } = useToast()

const cardDisplay = ref('')
const expiryMonth = ref(null)
const expiryYear = ref(null)
const cardholder = ref('')
const processing = ref(false)
const errors = ref({ cardNumber: '', expiry: '' })

const yearOptions = computed(() => {
  const current = new Date().getFullYear()
  return Array.from({ length: 15 }, (_, i) => current + i)
})

onMounted(() => {
  if (cartItems.value.length === 0) {
    router.replace('/')
  }
})

function formatCardNumber(e) {
  let v = e.target.value.replace(/\D/g, '')
  if (v.length > 19) v = v.slice(0, 19)
  cardDisplay.value = v.replace(/(\d{4})(?=\d)/g, '$1 ').trim()
}

function validate() {
  errors.value = { cardNumber: '', expiry: '' }
  let ok = true

  const digits = cardDisplay.value.replace(/\D/g, '')
  if (digits.length < 13 || digits.length > 19) {
    errors.value.cardNumber = 'La tarjeta debe tener entre 13 y 19 dígitos'
    ok = false
  }

  const month = expiryMonth.value
  const year = expiryYear.value
  if (month == null || year == null) {
    errors.value.expiry = 'Indica mes y año de vencimiento'
    ok = false
  } else {
    const now = new Date()
    const currentYear = now.getFullYear()
    const currentMonth = now.getMonth() + 1
    const y = year < 100 ? 2000 + year : year
    if (y < currentYear || (y === currentYear && month < currentMonth)) {
      errors.value.expiry = 'La tarjeta está vencida'
      ok = false
    }
  }
  return ok
}

async function submitPayment() {
  if (!validate()) return
  if (!user.value) {
    showError('Debes iniciar sesión')
    router.push('/login')
    return
  }

  processing.value = true
  try {
    const items = cartItems.value.map(item => ({ partId: item.partId, qty: item.qty }))
    const payment = {
      cardNumber: cardDisplay.value.replace(/\D/g, ''),
      expiryMonth: expiryMonth.value,
      expiryYear: expiryYear.value
    }
    const order = await createOrder(user.value.userId, items, payment)
    success('¡Pago procesado! Revisa tu correo para el detalle del pedido.')
    clearCart()
    router.push(`/mis-pedidos/${order.orderId}`)
  } catch (e) {
    showError(e.message || 'Error al procesar el pago')
  } finally {
    processing.value = false
  }
}
</script>

<style scoped>
.checkout-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 28px;
}

.page-header h1 {
  font-size: 28px;
  color: #1a1a2e;
  margin-bottom: 4px;
}

.subtitle {
  color: #64748b;
  font-size: 14px;
}

.empty-checkout {
  text-align: center;
  padding: 60px 20px;
}

.empty-checkout p {
  margin-bottom: 16px;
  color: #64748b;
}

.checkout-content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 32px;
  align-items: start;
}

@media (max-width: 900px) {
  .checkout-content {
    grid-template-columns: 1fr;
  }
}

.order-summary-section,
.payment-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  margin-bottom: 24px;
}

.order-summary-section h2,
.payment-section h2 {
  font-size: 18px;
  color: #1a1a2e;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  display: grid;
  grid-template-columns: 56px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.order-item:last-child {
  border-bottom: none;
}

.item-thumb {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: #f1f5f9;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-img {
  font-size: 24px;
  color: #94a3b8;
}

.item-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-title {
  font-weight: 600;
  color: #1a1a2e;
}

.item-meta {
  font-size: 12px;
  color: #64748b;
}

.item-line-total {
  font-weight: 600;
  color: #0f766e;
}

.payment-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.form-group input,
.form-group select {
  padding: 12px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 15px;
  transition: border-color 0.2s;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #0d9488;
  box-shadow: 0 0 0 3px rgba(13, 148, 136, 0.15);
}

.form-group input.error,
.form-group select.error {
  border-color: #dc2626;
}

.field-error {
  font-size: 12px;
  color: #dc2626;
}

.hint {
  font-size: 12px;
  color: #94a3b8;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.btn-pay {
  margin-top: 8px;
  padding: 14px;
  font-size: 16px;
  border-radius: 8px;
  background: #0d9488;
}

.btn-pay:hover:not(:disabled) {
  background: #0f766e;
}

.btn-pay:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.checkout-sidebar {
  position: sticky;
  top: 24px;
}

.sidebar-box {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e2e8f0;
}

.sidebar-box h3 {
  font-size: 16px;
  color: #1a1a2e;
  margin-bottom: 16px;
}

.sidebar-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  color: #475569;
}

.sidebar-row.total {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
}

.secure-note {
  margin-top: 12px;
  font-size: 12px;
  color: #64748b;
}

.btn {
  padding: 12px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: #0d9488;
  color: white;
}
</style>
