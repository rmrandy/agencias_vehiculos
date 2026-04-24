/**
 * Precio con descuento porcentual (alineado con useEnterpriseDiscount).
 * @param {number|string|null|undefined} precio
 * @param {number|string|null|undefined} discountPercent porcentaje 0–100
 * @returns {number|null}
 */
export function priceWithDiscount(precio, discountPercent) {
  if (precio == null) return null
  const p = Number(precio)
  if (discountPercent == null || Number(discountPercent) <= 0) return p
  const d = Number(discountPercent) / 100
  return Math.round(p * (1 - d) * 100) / 100
}
