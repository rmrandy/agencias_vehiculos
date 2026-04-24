import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { getHealth } from './health.js'

describe('getHealth', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('devuelve el cuerpo JSON cuando HTTP es OK', async () => {
    globalThis.fetch.mockResolvedValue({
      ok: true,
      json: async () => ({ status: 'UP' }),
    })
    const body = await getHealth()
    expect(body.status).toBe('UP')
  })
})
