import { ref, computed, watch } from 'vue'
import { useAuth } from './useAuth'
import { getEmpresarial } from '../api/usuarios'

const cachedProfile = ref(null)
const cachedUserId = ref(null)

/**
 * Composable para el descuento de usuario empresarial.
 * Si el usuario está logueado y es empresarial, carga su perfil y expone:
 * - discountPercent: número (ej. 10) o null
 * - hasDiscount: boolean
 * - precioConDescuento(precio): devuelve el precio con descuento aplicado
 * - precioOriginal: para mostrar tachado cuando hay descuento
 */
export function useEnterpriseDiscount() {
  const { user, isLoggedIn, isEnterprise } = useAuth()
  const loading = ref(false)
  const error = ref(null)

  const profile = ref(null)

  async function loadProfile() {
    if (!user.value?.userId || !isEnterprise.value) {
      profile.value = null
      return
    }
    if (cachedUserId.value === user.value.userId && cachedProfile.value) {
      profile.value = cachedProfile.value
      return
    }
    loading.value = true
    error.value = null
    try {
      const p = await getEmpresarial(user.value.userId)
      profile.value = p
      cachedProfile.value = p
      cachedUserId.value = user.value.userId
    } catch (e) {
      if (e.message && !e.message.includes('404')) {
        error.value = e.message
      }
      profile.value = null
    } finally {
      loading.value = false
    }
  }

  watch([user, isEnterprise], () => {
    if (isLoggedIn.value && isEnterprise.value) {
      loadProfile()
    } else {
      profile.value = null
    }
  }, { immediate: true })

  const discountPercent = computed(() => profile.value?.discountPercent ?? null)
  const hasDiscount = computed(() => discountPercent.value != null && Number(discountPercent.value) > 0)

  function precioConDescuento(precio) {
    if (precio == null) return null
    const p = Number(precio)
    if (!hasDiscount.value) return p
    const d = Number(discountPercent.value) / 100
    return Math.round(p * (1 - d) * 100) / 100
  }

  return {
    discountPercent,
    hasDiscount,
    precioConDescuento,
    loading,
    error,
    loadProfile,
  }
}
