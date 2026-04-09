import { apiFetch } from './config'
import type { CatalogPart } from './repuestos'
import type { Comentario } from './comentarios'

export async function getRepuestoFabrica(proveedorId: number, partId: number): Promise<CatalogPart> {
  return apiFetch(`/api/repuestos/fabrica/${proveedorId}/${partId}`)
}

export async function getComentariosFabrica(proveedorId: number, partId: number): Promise<Comentario[]> {
  return apiFetch(`/api/repuestos/fabrica/${proveedorId}/${partId}/comentarios`)
}

export async function createComentarioFabrica(
  proveedorId: number,
  partId: number,
  body: {
    userEmail: string
    userFullName?: string
    parentId?: number
    rating?: number
    body: string
  }
): Promise<Comentario> {
  return apiFetch(`/api/repuestos/fabrica/${proveedorId}/${partId}/comentarios`, {
    method: 'POST',
    body: JSON.stringify({
      userEmail: body.userEmail,
      userFullName: body.userFullName,
      parentId: body.parentId,
      rating: body.rating,
      body: body.body,
    }),
  })
}
