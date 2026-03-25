export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

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
