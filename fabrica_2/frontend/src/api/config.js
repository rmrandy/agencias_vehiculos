/**
 * URL base del backend Java/Jersey de la fábrica (sin path `/api`; las rutas lo incluyen en cada llamada).
 * @type {string}
 */
const viteUrl = typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_URL
const processUrl = typeof process !== 'undefined' && process.env?.VITE_API_URL
export const API_URL = (viteUrl || processUrl || 'http://localhost:8080').replace(/\/$/, '')

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
