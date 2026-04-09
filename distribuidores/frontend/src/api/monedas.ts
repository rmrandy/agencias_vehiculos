import { apiFetch } from './config'

export interface MonedaRow {
  code: string
  name: string
  symbol: string
  unitsPerUsd: number
}

export async function getMonedas(): Promise<MonedaRow[]> {
  return apiFetch('/api/monedas')
}

export async function putMonedaTasa(
  code: string,
  unitsPerUsd: number,
  adminUserId: number
): Promise<MonedaRow> {
  return apiFetch(`/api/monedas/${encodeURIComponent(code)}`, {
    method: 'PUT',
    body: JSON.stringify({ adminUserId, unitsPerUsd }),
  })
}
