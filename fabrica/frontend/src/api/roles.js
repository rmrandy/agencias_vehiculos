import { apiFetch } from './config'

export async function listRoles() {
  return apiFetch('/api/roles')
}
