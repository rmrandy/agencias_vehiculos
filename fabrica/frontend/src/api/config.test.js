import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { apiFetch } from './config.js'

describe('apiFetch', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('devuelve el JSON cuando la respuesta es OK', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: true,
      json: async () => ({ ok: true }),
    })
    const data = await apiFetch('/api/health')
    expect(data.ok).toBe(true)
    expect(globalThis.fetch).toHaveBeenCalled()
  })

  it('lanza Error con el mensaje del servidor cuando no es OK', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({ message: 'Error interno' }),
    })
    await expect(apiFetch('/api/x')).rejects.toThrow('Error interno')
  })
})
