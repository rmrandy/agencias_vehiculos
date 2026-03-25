import { ref, computed } from 'vue'

const cartItems = ref([])

// Cargar carrito desde localStorage
const loadCart = () => {
  const saved = localStorage.getItem('cart')
  if (saved) {
    try {
      cartItems.value = JSON.parse(saved)
    } catch (e) {
      cartItems.value = []
    }
  }
}

// Guardar carrito en localStorage
const saveCart = () => {
  localStorage.setItem('cart', JSON.stringify(cartItems.value))
}

// Inicializar
loadCart()

export function useCart() {
  const addToCart = (part, qty = 1) => {
    const existing = cartItems.value.find(item => item.partId === part.partId)
    
    if (existing) {
      existing.qty += qty
    } else {
      cartItems.value.push({
        partId: part.partId,
        title: part.title,
        partNumber: part.partNumber,
        price: part.price,
        qty: qty,
        hasImage: part.hasImage,
        imageType: part.imageType
      })
    }
    
    saveCart()
  }

  const removeFromCart = (partId) => {
    const index = cartItems.value.findIndex(item => item.partId === partId)
    if (index > -1) {
      cartItems.value.splice(index, 1)
      saveCart()
    }
  }

  const updateQuantity = (partId, qty) => {
    const item = cartItems.value.find(item => item.partId === partId)
    if (item) {
      item.qty = qty
      if (item.qty <= 0) {
        removeFromCart(partId)
      } else {
        saveCart()
      }
    }
  }

  const clearCart = () => {
    cartItems.value = []
    saveCart()
  }

  const cartTotal = computed(() => {
    return cartItems.value.reduce((total, item) => {
      return total + (item.price * item.qty)
    }, 0)
  })

  const cartCount = computed(() => {
    return cartItems.value.reduce((count, item) => count + item.qty, 0)
  })

  return {
    cartItems,
    cartTotal,
    cartCount,
    addToCart,
    removeFromCart,
    updateQuantity,
    clearCart
  }
}
