const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5080'

export interface HealthResponse {
  status: string
}

export async function getHealth(): Promise<HealthResponse> {
  const res = await fetch(`${API_URL}/api/health`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}
