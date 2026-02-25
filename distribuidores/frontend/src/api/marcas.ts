import { apiFetch } from './config'

export interface Brand {
  brandId: number
  name: string
}

export async function listMarcas(): Promise<Brand[]> {
  return apiFetch('/api/marcas')
}

export async function getMarca(id: number): Promise<Brand> {
  return apiFetch(`/api/marcas/${id}`)
}
