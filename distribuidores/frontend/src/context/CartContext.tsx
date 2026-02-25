import { createContext, useContext, useState, ReactNode } from 'react'
import type { Part } from '../api/repuestos'

export type CartItem = { part: Part; qty: number }

type CartContextType = {
  items: CartItem[]
  add: (part: Part, qty: number) => void
  remove: (partId: number) => void
  setQty: (partId: number, qty: number) => void
  clear: () => void
  count: number
}

const CartContext = createContext<CartContextType | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([])

  const add = (part: Part, qty: number) => {
    setItems((prev) => {
      const i = prev.findIndex((x) => x.part.partId === part.partId)
      if (i >= 0) {
        const next = [...prev]
        next[i].qty += qty
        return next
      }
      return [...prev, { part, qty }]
    })
  }

  const remove = (partId: number) => {
    setItems((prev) => prev.filter((x) => x.part.partId !== partId))
  }

  const setQty = (partId: number, qty: number) => {
    if (qty <= 0) {
      setItems((prev) => prev.filter((x) => x.part.partId !== partId))
      return
    }
    setItems((prev) =>
      prev.map((x) => (x.part.partId === partId ? { ...x, qty } : x))
    )
  }

  const clear = () => setItems([])

  const count = items.reduce((s, x) => s + x.qty, 0)

  return (
    <CartContext.Provider value={{ items, add, remove, setQty, clear, count }}>
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within CartProvider')
  return ctx
}
