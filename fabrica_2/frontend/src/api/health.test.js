import { describe, it, beforeEach, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { getHealth } from './health.js'

describe('getHealth', () => {
  let prevFetch

  beforeEach(() => {
    prevFetch = globalThis.fetch
  })

  afterEach(() => {
    globalThis.fetch = prevFetch
  })

  it('devuelve el cuerpo JSON cuando HTTP es OK', async () => {
    globalThis.fetch = async () => ({
      ok: true,
      json: async () => ({ status: 'UP' }),
    })
    const body = await getHealth()
    assert.equal(body.status, 'UP')
  })
})
