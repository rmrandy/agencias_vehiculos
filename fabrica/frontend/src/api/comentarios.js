import { apiFetch } from './config'

/**
 * Obtiene comentarios y promedio de valoración de un repuesto.
 * @returns { Promise<{ promedio: number | null, comentarios: Array }> }
 */
export async function getComentarios(partId) {
  return apiFetch(`/api/repuestos/${partId}/comentarios`)
}

/**
 * Crea un comentario o una respuesta.
 * @param { number } partId
 * @param { { userId: number, body: string, rating?: number, parentId?: number } } data
 */
export async function createComentario(partId, data) {
  return apiFetch(`/api/repuestos/${partId}/comentarios`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}
