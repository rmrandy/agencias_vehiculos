<script setup>
import { ref, onMounted, computed } from 'vue'
import { useAuth } from '../composables/useAuth'
import { useToast } from '../composables/useToast'
import { listUsuarios, assignRoles, assignEmpresarial, unassignEmpresarial, getEmpresarial } from '../api/usuarios'
import { listRoles } from '../api/roles'

const { user } = useAuth()
const { success, error: showError } = useToast()

const usuarios = ref([])
const roles = ref([])
const loading = ref(true)
const error = ref('')
const modalUser = ref(null)
const selectedRoleIds = ref([])
const savingRoles = ref(false)
const rolesError = ref('')

const modalEmpresarial = ref(null)
const empresarialDiscount = ref(10)
const savingEmpresarial = ref(false)
const empresarialError = ref('')

const adminId = computed(() => user.value?.userId ?? null)

onMounted(async () => {
  try {
    const [u, r] = await Promise.all([listUsuarios(), listRoles()])
    usuarios.value = u
    roles.value = r
  } catch (e) {
    error.value = e.message || 'Error al cargar datos'
  } finally {
    loading.value = false
  }
})

function openRolesModal(usuario) {
  modalUser.value = usuario
  const names = usuario.roles || []
  selectedRoleIds.value = roles.value.filter((r) => names.includes(r.name)).map((r) => r.roleId)
  rolesError.value = ''
}

function closeModal() {
  modalUser.value = null
  selectedRoleIds.value = []
  rolesError.value = ''
}

function toggleRole(roleId) {
  const id = Number(roleId)
  const idx = selectedRoleIds.value.indexOf(id)
  if (idx === -1) {
    selectedRoleIds.value.push(id)
  } else {
    selectedRoleIds.value.splice(idx, 1)
  }
}

function hasRole(roleId) {
  return selectedRoleIds.value.includes(Number(roleId))
}

async function saveRoles() {
  if (!modalUser.value || adminId.value == null) return
  rolesError.value = ''
  savingRoles.value = true
  try {
    const updated = await assignRoles(adminId.value, modalUser.value.userId, selectedRoleIds.value)
    const idx = usuarios.value.findIndex((u) => u.userId === updated.userId)
    if (idx !== -1) usuarios.value[idx] = updated
    success(`Roles actualizados para ${modalUser.value.email}`)
    closeModal()
  } catch (e) {
    rolesError.value = e.message || 'Error al guardar roles'
    showError(rolesError.value)
  } finally {
    savingRoles.value = false
  }
}

function formatRoles(usuario) {
  const r = usuario.roles || []
  return r.length ? r.join(', ') : '—'
}

function openEmpresarialModal(usuario) {
  modalEmpresarial.value = usuario
  empresarialDiscount.value = usuario.enterpriseDiscountPercent ?? 10
  empresarialError.value = ''
}

function closeEmpresarialModal() {
  modalEmpresarial.value = null
  empresarialError.value = ''
}

async function saveEmpresarial() {
  if (!modalEmpresarial.value || adminId.value == null) return
  empresarialError.value = ''
  savingEmpresarial.value = true
  try {
    await assignEmpresarial(adminId.value, modalEmpresarial.value.userId, empresarialDiscount.value)
    const list = await listUsuarios()
    usuarios.value = list
    success(`Usuario ${modalEmpresarial.value.email} asignado como empresarial con ${empresarialDiscount.value}% de descuento`)
    closeEmpresarialModal()
  } catch (e) {
    empresarialError.value = e.message || 'Error'
    showError(empresarialError.value)
  } finally {
    savingEmpresarial.value = false
  }
}

async function quitarEmpresarial(usuario) {
  if (!adminId.value || !confirm(`¿Quitar perfil empresarial de ${usuario.email}?`)) return
  try {
    await unassignEmpresarial(adminId.value, usuario.userId)
    const list = await listUsuarios()
    usuarios.value = list
    success('Perfil empresarial quitado')
  } catch (e) {
    showError(e.message || 'Error al quitar perfil empresarial')
  }
}
</script>

<template>
  <div class="usuarios-page">
    <header class="page-header">
      <h1>Usuarios</h1>
      <p class="page-subtitle">Gestionar usuarios y asignar roles (solo administradores)</p>
    </header>

    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-else-if="loading" class="loading">Cargando usuarios…</div>

    <div v-else class="table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Email</th>
            <th>Nombre</th>
            <th>Teléfono</th>
            <th>Roles</th>
            <th>Empresarial</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in usuarios" :key="u.userId">
            <td>{{ u.userId }}</td>
            <td>{{ u.email }}</td>
            <td>{{ u.fullName || '—' }}</td>
            <td>{{ u.phone || '—' }}</td>
            <td><span class="badges">{{ formatRoles(u) }}</span></td>
            <td>
              <span v-if="u.isEnterprise" class="badge-emp">
                {{ u.enterpriseDiscountPercent != null ? u.enterpriseDiscountPercent + '% desc.' : 'Sí' }}
              </span>
              <span v-else>—</span>
            </td>
            <td class="actions-cell">
              <button type="button" class="btn btn-sm btn-outline" @click="openRolesModal(u)">
                Roles
              </button>
              <template v-if="u.isEnterprise">
                <button type="button" class="btn btn-sm btn-outline" @click="openEmpresarialModal(u)">
                  Editar descuento
                </button>
                <button type="button" class="btn btn-sm btn-outline btn-danger" @click="quitarEmpresarial(u)">
                  Quitar empresarial
                </button>
              </template>
              <button v-else type="button" class="btn btn-sm btn-primary" @click="openEmpresarialModal(u)">
                Hacer empresarial
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal perfil empresarial -->
    <div v-if="modalEmpresarial" class="modal-backdrop" @click.self="closeEmpresarialModal">
      <div class="modal">
        <div class="modal-header">
          <h2>Perfil empresarial — {{ modalEmpresarial.email }}</h2>
          <button type="button" class="modal-close" aria-label="Cerrar" @click="closeEmpresarialModal">×</button>
        </div>
        <div class="modal-body">
          <p v-if="empresarialError" class="alert alert-error">{{ empresarialError }}</p>
          <div class="form-group">
            <label>Descuento (%)</label>
            <input v-model.number="empresarialDiscount" type="number" min="0" max="99" step="0.5" />
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline" @click="closeEmpresarialModal">Cancelar</button>
          <button type="button" class="btn btn-primary" :disabled="savingEmpresarial" @click="saveEmpresarial">
            {{ savingEmpresarial ? 'Guardando…' : 'Asignar / Actualizar' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Modal asignar roles -->
    <div v-if="modalUser" class="modal-backdrop" @click.self="closeModal">
      <div class="modal">
        <div class="modal-header">
          <h2>Asignar roles — {{ modalUser.email }}</h2>
          <button type="button" class="modal-close" aria-label="Cerrar" @click="closeModal">×</button>
        </div>
        <div class="modal-body">
          <p v-if="rolesError" class="alert alert-error">{{ rolesError }}</p>
          <div class="roles-list">
            <label v-for="r in roles" :key="r.roleId" class="role-check">
              <input type="checkbox" :checked="hasRole(r.roleId)" @change="toggleRole(r.roleId)" />
              <span>{{ r.name }}</span>
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline" @click="closeModal">Cancelar</button>
          <button type="button" class="btn btn-primary" :disabled="savingRoles" @click="saveRoles">
            {{ savingRoles ? 'Guardando…' : 'Guardar' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.usuarios-page {
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
.badges {
  color: #475569;
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
.btn-sm { padding: 0.35rem 0.6rem; font-size: 0.8125rem; }
.btn-outline {
  background: transparent;
  border: 1px solid #cbd5e1;
  color: #475569;
}
.btn-outline:hover {
  background: #f1f5f9;
  color: #0f172a;
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

/* Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 2rem;
}
.modal {
  background: #fff;
  border-radius: var(--radius);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  max-width: 420px;
  width: 100%;
  max-height: 90vh;
  overflow: auto;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
}
.modal-header h2 {
  font-size: 1.125rem;
  font-weight: 600;
  margin: 0;
  color: #0f172a;
}
.modal-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  color: #64748b;
  cursor: pointer;
  padding: 0.25rem;
}
.modal-close:hover {
  color: #0f172a;
}
.modal-body {
  padding: 1.5rem;
}
.roles-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.role-check {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.9375rem;
}
.role-check input {
  width: 1rem;
  height: 1rem;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #e2e8f0;
}
.badge-emp {
  background: #dbeafe;
  color: #1d4ed8;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.8125rem;
}
.actions-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}
.btn-danger {
  color: #b91c1c;
  border-color: #fecaca;
}
.btn-danger:hover {
  background: #fee2e2;
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
.form-group input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-sm);
}
</style>
