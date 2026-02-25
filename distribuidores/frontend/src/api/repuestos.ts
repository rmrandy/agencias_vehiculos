import { apiFetch } from './config'

export interface Part {
  partId: number
  categoryId?: number
  brandId?: number
  partNumber: string
  title: string
  description?: string
  weightLb?: number
  price: number
  active?: number
  createdAt?: string
  inStock?: boolean
  availableQuantity?: number
  hasImage?: boolean
  lowStock?: boolean
  stockQuantity?: number
}

export interface CreateRepuestoBody {
  categoryId: number
  brandId: number
  partNumber: string
  title: string
  description?: string
  weightLb?: number
  price: number
  stockQuantity?: number
  lowStockThreshold?: number
  imageData?: string
  imageType?: string
}

export interface UpdateRepuestoBody {
  categoryId?: number
  brandId?: number
  title?: string
  description?: string
  weightLb?: number
  price?: number
  active?: number
  stockQuantity?: number
  lowStockThreshold?: number
  imageData?: string
  imageType?: string
}

export async function listRepuestos(params?: { categoryId?: number; brandId?: number; includeInactive?: boolean }): Promise<Part[]> {
  const q = new URLSearchParams()
  if (params?.categoryId) q.set('categoryId', String(params.categoryId))
  if (params?.brandId) q.set('brandId', String(params.brandId))
  if (params?.includeInactive) q.set('includeInactive', 'true')
  const qs = q.toString()
  return apiFetch(`/api/repuestos${qs ? '?' + qs : ''}`)
}

export async function buscarRepuestos(params?: {
  nombre?: string
  descripcion?: string
  especificaciones?: string
}): Promise<Part[]> {
  const q = new URLSearchParams()
  if (params?.nombre) q.set('nombre', params.nombre)
  if (params?.descripcion) q.set('descripcion', params.descripcion)
  if (params?.especificaciones) q.set('especificaciones', params.especificaciones)
  const qs = q.toString()
  return apiFetch(`/api/repuestos/busqueda${qs ? '?' + qs : ''}`)
}

export async function getRepuesto(id: number): Promise<Part> {
  return apiFetch(`/api/repuestos/${id}`)
}

export async function createRepuesto(body: CreateRepuestoBody): Promise<Part> {
  return apiFetch('/api/repuestos', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export async function updateRepuesto(id: number, body: UpdateRepuestoBody): Promise<Part> {
  return apiFetch(`/api/repuestos/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export async function deleteRepuesto(id: number): Promise<void> {
  await apiFetch(`/api/repuestos/${id}`, { method: 'DELETE' })
}

export async function getGaleria(partId: number): Promise<{ count: number }> {
  return apiFetch(`/api/repuestos/${partId}/galeria`)
}

export function getPartImageUrl(partId: number, index: number): string {
  const base = (import.meta.env.VITE_API_URL || 'http://localhost:5080').replace(/\/$/, '')
  return `${base}/api/images/part/${partId}/imagen/${index}`
}

export async function addImagenToProduct(partId: number, imageData: string, imageType?: string): Promise<{ count: number }> {
  const strip = imageData.includes(',') ? imageData.split(',')[1]! : imageData
  return apiFetch(`/api/repuestos/${partId}/imagenes`, {
    method: 'POST',
    body: JSON.stringify({ imageData: strip, imageType }),
  })
}
