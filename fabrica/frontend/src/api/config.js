/**
 * URL base del backend Java/Jersey de la fábrica (sin path `/api`; las rutas lo incluyen en cada llamada).
 * @type {string}
 */
export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

/**
 * Petición JSON al API de la fábrica; rechaza la promesa con `Error` si la respuesta HTTP no es OK.
 * @param {string} path Ruta completa incluyendo prefijo `/api/...`.
 * @param {RequestInit} [options] Opciones de `fetch`.
 * @returns {Promise<any>} Cuerpo JSON parseado.
 */
export function apiFetch(path, options = {}) {
  const url = `${API_URL}${path}`
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  }
  return fetch(url, { ...options, headers }).then(async (res) => {
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      const msg = data.message || data.error || `Error ${res.status}`
      throw new Error(msg)
    }
    return data
  })
}
