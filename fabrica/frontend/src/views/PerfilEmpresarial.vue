<template>
  <div class="perfil-empresarial-page">
    <header class="page-header">
      <h1>Mi perfil empresarial</h1>
      <p class="page-subtitle">Configura tu dirección por defecto, tarjeta y horario de entrega. Ver compras y estado de envío.</p>
    </header>

    <div v-if="loading" class="loading">Cargando perfil...</div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>
    <div v-else-if="!profile" class="alert alert-error">No tienes perfil empresarial. Contacta al administrador para que te asigne como usuario empresarial.</div>

    <template v-else>
      <section class="card section-card">
        <h2>Configuración por defecto</h2>
        <form @submit.prevent="saveProfile" class="form">
          <div class="form-group">
            <label>Dirección de envío por defecto</label>
            <textarea v-model="form.defaultAddressText" rows="3" placeholder="Calle, número, ciudad, CP, país"></textarea>
          </div>
          <div class="form-group">
            <label>Tarjeta por defecto (últimos 4 dígitos, solo para mostrar)</label>
            <input v-model="form.defaultCardLast4" type="text" maxlength="4" placeholder="1234" />
          </div>
          <div class="form-group">
            <label>Horario de entrega preferido</label>
            <input v-model="form.deliveryWindow" type="text" placeholder="Ej: Lun-Vie 9:00-18:00" />
          </div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="saving">
              {{ saving ? 'Guardando…' : 'Guardar configuración' }}
            </button>
          </div>
        </form>
      </section>

      <section class="card section-card">
        <h2>Compras y envíos</h2>
        <p>Consulta tus pedidos y el estado de envío.</p>
        <router-link to="/mis-pedidos" class="btn btn-primary">Ver mis pedidos</router-link>
      </section>

      <section v-if="profile.discountPercent != null" class="card section-card highlight">
        <p class="discount-info">Tu cuenta tiene un <strong>{{ profile.discountPercent }}% de descuento</strong> aplicado en cada compra.</p>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuth } from '../composables/useAuth'
import { useToast } from '../composables/useToast'
import { getEmpresarial, updateMiPerfilEmpresarial } from '../api/usuarios'

const { user } = useAuth()
const { success, error: showError } = useToast()

const profile = ref(null)
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const form = ref({
  defaultAddressText: '',
  defaultCardLast4: '',
  deliveryWindow: '',
})

onMounted(async () => {
  if (!user.value?.userId) {
    error.value = 'Debes iniciar sesión'
    loading.value = false
    return
  }
  try {
    profile.value = await getEmpresarial(user.value.userId)
    form.value = {
      defaultAddressText: profile.value.defaultAddressText || '',
      defaultCardLast4: profile.value.defaultCardLast4 || '',
      deliveryWindow: profile.value.deliveryWindow || '',
    }
  } catch (e) {
    if (e.message && e.message.includes('404')) {
      profile.value = null
    } else {
      error.value = e.message || 'Error al cargar perfil'
    }
  } finally {
    loading.value = false
  }
})

async function saveProfile() {
  if (!user.value?.userId || !profile.value) return
  saving.value = true
  try {
    profile.value = await updateMiPerfilEmpresarial(user.value.userId, {
      defaultAddressText: form.value.defaultAddressText || null,
      defaultCardLast4: form.value.defaultCardLast4 || null,
      deliveryWindow: form.value.deliveryWindow || null,
    })
    success('Configuración guardada')
  } catch (e) {
    showError(e.message || 'Error al guardar')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.perfil-empresarial-page {
  max-width: 700px;
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
.card {
  background: var(--card-bg);
  border-radius: var(--radius);
  border: 1px solid #e2e8f0;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}
.section-card h2 {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0 0 1rem;
  color: #0f172a;
}
.form-group {
  margin-bottom: 1rem;
}
.form-group label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.35rem;
  font-size: 0.9375rem;
}
.form-group input,
.form-group textarea {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-sm);
  box-sizing: border-box;
}
.form-actions {
  margin-top: 1rem;
}
.btn {
  padding: 0.5rem 1rem;
  font-size: 0.9375rem;
  font-weight: 500;
  border-radius: var(--radius-sm);
  cursor: pointer;
  border: none;
  text-decoration: none;
  display: inline-block;
}
.btn-primary {
  background: var(--sidebar-bg);
  color: #fff;
}
.btn-primary:hover:not(:disabled) {
  background: var(--sidebar-hover);
}
.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.alert { padding: 1rem; border-radius: var(--radius-sm); margin-bottom: 1rem; }
.alert-error { background: #fee2e2; color: #b91c1c; }
.loading { padding: 2rem; color: #64748b; }
.highlight { background: #f0fdf4; border-color: #86efac; }
.discount-info { margin: 0; font-size: 0.9375rem; color: #166534; }
</style>
