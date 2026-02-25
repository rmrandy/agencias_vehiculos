export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5080'

export async function apiFetch(path: string, options: RequestInit = {}): Promise<any> {
  const url = `${API_URL}${path}`
  const res = await fetch(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers },
  })
  const data = res.status === 204 ? {} : await res.json().catch(() => ({}))
  if (!res.ok) {
    const msg = (data as { message?: string }).message || (data as { error?: string }).error || `Error ${res.status}`
    throw new Error(msg)
  }
  return res.status === 204 ? undefined : data
}
