import { apiFetch } from './config'

export interface Category {
  categoryId: number
  name: string
  parentId?: number
}

export async function listCategorias(): Promise<Category[]> {
  return apiFetch('/api/categorias')
}

export async function getCategoria(id: number): Promise<Category> {
  return apiFetch(`/api/categorias/${id}`)
}
