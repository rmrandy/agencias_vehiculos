import { describe, it, beforeEach, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { apiFetch } from './config.js'

describe('apiFetch', () => {
  let prevFetch

  beforeEach(() => {
    prevFetch = globalThis.fetch
  })

  afterEach(() => {
    globalThis.fetch = prevFetch
  })

  it('devuelve el JSON cuando la respuesta es OK', async () => {
    let calledUrl = ''
    globalThis.fetch = async (url) => {
      calledUrl = String(url)
      return { ok: true, json: async () => ({ ok: true }) }
    }
    const data = await apiFetch('/api/health')
    assert.equal(data.ok, true)
    assert.ok(calledUrl.includes('/api/health'))
  })

  it('lanza Error con el mensaje del servidor cuando no es OK', async () => {
    globalThis.fetch = async () => ({
      ok: false,
      status: 500,
      json: async () => ({ message: 'Error interno' }),
    })
    await assert.rejects(apiFetch('/api/x'), (err) => {
      assert.ok(err instanceof Error)
      assert.equal(err.message, 'Error interno')
      return true
    })
  })
})
