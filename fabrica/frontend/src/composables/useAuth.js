import { ref, computed } from 'vue'

const STORAGE_KEY = 'fabrica_user'

function getStoredUser() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

const currentUser = ref(getStoredUser())

export function useAuth() {
  const user = computed(() => currentUser.value)

  const isLoggedIn = computed(() => !!currentUser.value)

  const isAdmin = computed(() => {
    const roles = currentUser.value?.roles || []
    return roles.includes('ADMIN')
  })

  const isEnterprise = computed(() => {
    const roles = currentUser.value?.roles || []
    return roles.includes('ENTERPRISE')
  })

  function setUser(userData) {
    currentUser.value = userData
    if (userData) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(userData))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  function logout() {
    setUser(null)
  }

  return { user, isLoggedIn, isAdmin, isEnterprise, setUser, logout }
}
