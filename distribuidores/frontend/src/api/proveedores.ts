import { apiFetch } from './config'

export interface ProveedorDto {
  proveedorId: number
  nombre: string
  contacto?: string | null
  email?: string | null
  telefono?: string | null
  apiBaseUrl?: string | null
  fabricaEnterpriseUserId?: number | null
  tipoCambioAQuetzales?: number | null
  porcentajeGanancia?: number | null
  costoEnvioPorLibra?: number | null
  activo: boolean
  esInternacional?: boolean
}

export interface SaveProveedorBody {
  nombre: string
  contacto?: string | null
  email?: string | null
  telefono?: string | null
  apiBaseUrl?: string | null
  fabricaEnterpriseUserId?: number | null
  tipoCambioAQuetzales?: number | null
  porcentajeGanancia?: number | null
  costoEnvioPorLibra?: number | null
  activo?: boolean
}

export async function listProveedores(incluirInactivos = true): Promise<ProveedorDto[]> {
  const q = incluirInactivos ? '' : '?incluirInactivos=false'
  return apiFetch(`/api/proveedores${q}`)
}

export async function getProveedor(id: number): Promise<ProveedorDto> {
  return apiFetch(`/api/proveedores/${id}`)
}

export async function createProveedor(body: SaveProveedorBody): Promise<ProveedorDto> {
  return apiFetch('/api/proveedores', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export async function updateProveedor(id: number, body: SaveProveedorBody): Promise<ProveedorDto> {
  return apiFetch(`/api/proveedores/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export async function deleteProveedor(id: number): Promise<void> {
  await apiFetch(`/api/proveedores/${id}`, { method: 'DELETE' })
}
