import { API_URL } from './config.js'

export async function getHealth() {
  const res = await fetch(`${API_URL}/api/health`)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}
