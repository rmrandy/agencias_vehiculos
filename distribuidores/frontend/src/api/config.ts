/**
 * URL base del backend .NET de la distribuidora (sin barra final).
 * Configurable con `VITE_API_URL` en `.env`; por defecto `http://localhost:5080`.
 */
export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:5080').replace(/\/$/, '')

/**
 * Ejecuta `fetch` contra el API de la distribuidora, serializa JSON y lanza {@link Error} con el mensaje del servidor si `!res.ok`.
 *
 * @param path Ruta absoluta del API (debe empezar por `/`, p. ej. `/api/health`).
 * @param options Opciones estándar de `fetch` (se fusiona `Content-Type: application/json`).
 * @returns Cuerpo parseado como JSON, o `undefined` si la respuesta es 204.
 */
export async function apiFetch(path: string, options: RequestInit = {}): Promise<any> {
  const url = `${API_URL}${path}`
  const res = await fetch(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  const data = res.status === 204 ? {} : await res.json().catch(() => ({}))
  if (!res.ok) {
    let msg =
      (data as { message?: string }).message || (data as { error?: string }).error || `Error ${res.status}`
    if (
      path.includes('catalogo/unificado') &&
      (res.status === 400 || res.status === 404 || res.status === 405)
    ) {
      msg +=
        ' El endpoint /api/repuestos/catalogo/unificado es del backend .NET de la distribuidora. ' +
        'Configura VITE_API_URL en .env (p. ej. http://localhost:5080). No uses el puerto del API Java de la fábrica (9090, 4040, etc.).'
    }
    throw new Error(msg)
  }
  return res.status === 204 ? undefined : data
}
