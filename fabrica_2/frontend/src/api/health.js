import { API_URL } from './config'

export async function getHealth() {
  const res = await fetch(`${API_URL}/api/health`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}
