import { describe, it, expect } from 'vitest'
import { priceWithDiscount } from './enterprisePricing'

describe('priceWithDiscount', () => {
  it('devuelve null si el precio es null', () => {
    expect(priceWithDiscount(null, 10)).toBeNull()
  })

  it('no aplica descuento si el porcentaje es 0 o null', () => {
    expect(priceWithDiscount(100, 0)).toBe(100)
    expect(priceWithDiscount(100, null)).toBe(100)
  })

  it('aplica 10% sobre 100 → 90', () => {
    expect(priceWithDiscount(100, 10)).toBe(90)
  })
})
