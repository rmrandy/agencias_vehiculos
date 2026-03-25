import { createContext, useContext, useState, ReactNode } from 'react'
import type { CatalogPart } from '../api/repuestos'
import { catalogLineKey } from '../api/repuestos'

export type CartItem = { part: CatalogPart; qty: number }

type CartContextType = {
  items: CartItem[]
  add: (part: CatalogPart, qty: number) => void
  removeLine: (lineKey: string) => void
  setQtyLine: (lineKey: string, qty: number) => void
  clear: () => void
  count: number
}

const CartContext = createContext<CartContextType | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([])

  const add = (part: CatalogPart, qty: number) => {
    const key = catalogLineKey(part)
    setItems((prev) => {
      const i = prev.findIndex((x) => catalogLineKey(x.part) === key)
      if (i >= 0) {
        const next = [...prev]
        next[i] = { ...next[i], qty: next[i].qty + qty }
        return next
      }
      return [...prev, { part, qty }]
    })
  }

  const removeLine = (lineKey: string) => {
    setItems((prev) => prev.filter((x) => catalogLineKey(x.part) !== lineKey))
  }

  const setQtyLine = (lineKey: string, qty: number) => {
    if (qty <= 0) {
      setItems((prev) => prev.filter((x) => catalogLineKey(x.part) !== lineKey))
      return
    }
    setItems((prev) =>
      prev.map((x) => (catalogLineKey(x.part) === lineKey ? { ...x, qty } : x))
    )
  }

  const clear = () => setItems([])

  const count = items.reduce((s, x) => s + x.qty, 0)

  return (
    <CartContext.Provider value={{ items, add, removeLine, setQtyLine, clear, count }}>
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within CartProvider')
  return ctx
}
