<script setup>
import { ref } from 'vue'
import { useAuth } from '../composables/useAuth'
import { useStoreInfo } from '../composables/useStoreInfo'
import { useToast } from '../composables/useToast'

const { isAdmin } = useAuth()
const { storeInfo, updateStoreInfo, resetStoreInfo } = useStoreInfo()
const { success } = useToast()

const form = ref({
  name: storeInfo.value.name,
  subtitle: storeInfo.value.subtitle,
  generalInfo: storeInfo.value.generalInfo,
})

function save() {
  updateStoreInfo(form.value)
  success('Encabezado actualizado')
}

function restoreDefaults() {
  resetStoreInfo()
  form.value = {
    name: 'Fabrica',
    subtitle: 'Agencias Vehiculos',
    generalInfo: 'Catalogo principal',
  }
  success('Valores por defecto restaurados')
}
</script>

<template>
  <div class="ajustes-page">
    <h1>Ajustes de tienda</h1>
    <p class="sub">Edita nombre y texto general del encabezado lateral.</p>

    <div v-if="!isAdmin" class="warn">Solo administradores pueden editar estos ajustes.</div>

    <form v-else class="form-grid" @submit.prevent="save">
      <div class="form-group">
        <label>Nombre de tienda</label>
        <input v-model="form.name" placeholder="Fabrica Central" />
      </div>
      <div class="form-group">
        <label>Subtitulo</label>
        <input v-model="form.subtitle" placeholder="Agencias Vehiculos" />
      </div>
      <div class="form-group">
        <label>Informacion general</label>
        <input v-model="form.generalInfo" placeholder="Repuestos multimarca" />
      </div>
      <div class="actions">
        <button class="btn btn-primary" type="submit">Guardar</button>
        <button class="btn btn-secondary" type="button" @click="restoreDefaults">Restaurar por defecto</button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.ajustes-page { max-width: 900px; }
.sub { color: #64748b; margin-top: 0; }
.warn { padding: .75rem 1rem; border: 1px solid #fecaca; background: #fff1f2; color: #9f1239; border-radius: 8px; }
.form-grid { display: grid; gap: 1rem; margin-top: 1rem; }
.form-group label { display: block; font-weight: 600; margin-bottom: .4rem; }
.form-group input { width: 100%; padding: .55rem .7rem; border-radius: 8px; border: 1px solid #cbd5e1; }
.actions { display: flex; gap: .75rem; }
</style>
