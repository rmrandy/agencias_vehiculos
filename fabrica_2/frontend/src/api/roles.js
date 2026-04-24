import { apiFetch } from './config.js'

export async function listRoles() {
  return apiFetch('/api/roles')
}
