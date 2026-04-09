import { apiFetch } from './config'

export interface ArancelPaisRow {
  countryCode: string
  countryName: string
  tariffPercent: number
}

export interface PaisLatamOption {
  countryCode: string
  countryName: string
}

export async function getAranceles(): Promise<ArancelPaisRow[]> {
  return apiFetch('/api/aranceles')
}

export async function getPaisesLatam(): Promise<PaisLatamOption[]> {
  return apiFetch('/api/aranceles/paises')
}

export async function putArancel(
  countryCode: string,
  tariffPercent: number,
  adminUserId: number
): Promise<ArancelPaisRow> {
  return apiFetch(`/api/aranceles/${encodeURIComponent(countryCode)}`, {
    method: 'PUT',
    body: JSON.stringify({ adminUserId, tariffPercent }),
  })
}

export async function getTarifaEnvioPorLibra(): Promise<{ usdPerLb: number }> {
  return apiFetch('/api/aranceles/envio/tarifa-por-libra')
}

export async function putTarifaEnvioPorLibra(
  usdPerLb: number,
  adminUserId: number
): Promise<{ usdPerLb: number }> {
  return apiFetch('/api/aranceles/envio/tarifa-por-libra', {
    method: 'PUT',
    body: JSON.stringify({ adminUserId, usdPerLb }),
  })
}
