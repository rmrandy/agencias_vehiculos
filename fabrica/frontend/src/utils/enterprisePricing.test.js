import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { priceWithDiscount } from './enterprisePricing.js'

describe('priceWithDiscount', () => {
  it('devuelve null si el precio es null', () => {
    assert.equal(priceWithDiscount(null, 10), null)
  })

  it('no aplica descuento si el porcentaje es 0 o null', () => {
    assert.equal(priceWithDiscount(100, 0), 100)
    assert.equal(priceWithDiscount(100, null), 100)
  })

  it('aplica 10% sobre 100 → 90', () => {
    assert.equal(priceWithDiscount(100, 10), 90)
  })
})
 