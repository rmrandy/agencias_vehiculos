import { apiFetch } from './config'

export interface Comentario {
  reviewId: number
  partId: number
  userId: number
  userEmail?: string
  fullName?: string
  parentId?: number
  rating?: number
  body: string
  createdAt?: string
  children: Comentario[]
}

export async function getComentarios(partId: number): Promise<Comentario[]> {
  return apiFetch(`/api/repuestos/${partId}/comentarios`)
}

export async function createComentario(
  partId: number,
  body: { userId: number; parentId?: number; rating?: number; body: string }
): Promise<Comentario> {
  return apiFetch(`/api/repuestos/${partId}/comentarios`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
