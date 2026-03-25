import { apiFetch } from './config'

export async function register(data) {
  return apiFetch('/api/usuarios', {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export async function listUsuarios() {
  return apiFetch('/api/usuarios')
}

export async function getUsuario(id) {
  return apiFetch(`/api/usuarios/${id}`)
}

export async function assignRoles(adminUserId, userId, roleIds) {
  return apiFetch(`/api/usuarios/${userId}/roles`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'X-Admin-User-Id': String(adminUserId),
    },
    body: JSON.stringify({ roleIds }),
  })
}

// Perfil empresarial
export async function getEmpresarial(userId) {
  return apiFetch(`/api/usuarios/${userId}/empresarial`)
}

export async function assignEmpresarial(adminUserId, userId, discountPercent) {
  return apiFetch(`/api/usuarios/${userId}/empresarial`, {
    method: 'PUT',
    body: JSON.stringify({ adminUserId, discountPercent }),
  })
}

export async function updateMiPerfilEmpresarial(userId, data) {
  return apiFetch(`/api/usuarios/${userId}/empresarial`, {
    method: 'PUT',
    body: JSON.stringify({ userId, ...data }),
  })
}

export async function unassignEmpresarial(adminUserId, userId) {
  return apiFetch(`/api/usuarios/${userId}/empresarial`, {
    method: 'DELETE',
    body: JSON.stringify({ adminUserId }),
  })
}
