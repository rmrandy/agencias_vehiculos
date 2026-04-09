import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getMonedas, type MonedaRow } from '../api/monedas'

const STORAGE_KEY = 'dist_currency'

type CurrencyContextType = {
  monedas: MonedaRow[]
  loading: boolean
  selectedCode: string
  setSelectedCode: (code: string) => void
  /** Precio de catálogo en USD → monto en la divisa elegida */
  fromUsd: (usd: number) => number
  /** Formato tienda/carrito (catálogo en USD) */
  formatCatalog: (usd: number) => string
  /** Pedidos ya guardados en `currencyCode` (sin convertir otra vez) */
  formatOrder: (amount: number, currencyCode: string) => string
}

const CurrencyContext = createContext<CurrencyContextType | null>(null)

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const [monedas, setMonedas] = useState<MonedaRow[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedCode, setSelectedCodeState] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEY) ?? 'USD'
    } catch {
      return 'USD'
    }
  })

  useEffect(() => {
    if (loading || monedas.length === 0) return
    if (!monedas.some((m) => m.code === selectedCode)) {
      setSelectedCodeState('USD')
      try {
        localStorage.setItem(STORAGE_KEY, 'USD')
      } catch {
        /* ignore */
      }
    }
  }, [loading, monedas, selectedCode])

  useEffect(() => {
    let cancelled = false
    getMonedas()
      .then((list) => {
        if (!cancelled) setMonedas(list)
      })
      .catch(() => {
        if (!cancelled) setMonedas([])
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const selected = useMemo(() => {
    const m = monedas.find((x) => x.code === selectedCode)
    if (m) return m
    return monedas.find((x) => x.code === 'USD') ?? { code: 'USD', name: 'USD', symbol: 'US$', unitsPerUsd: 1 }
  }, [monedas, selectedCode])

  const setSelectedCode = useCallback((code: string) => {
    const c = code.trim().toUpperCase()
    setSelectedCodeState(c)
    try {
      localStorage.setItem(STORAGE_KEY, c)
    } catch {
      /* ignore */
    }
  }, [])

  const fromUsd = useCallback(
    (usd: number) => {
      const mult = selected.unitsPerUsd ?? 1
      return Math.round(usd * mult * 100) / 100
    },
    [selected.unitsPerUsd]
  )

  const formatCatalog = useCallback(
    (usd: number) => {
      const v = fromUsd(usd)
      return `${selected.symbol}${v.toFixed(2)}`
    },
    [fromUsd, selected.symbol]
  )

  const formatOrder = useCallback(
    (amount: number, currencyCode: string) => {
      const code = (currencyCode ?? 'USD').toUpperCase()
      const row = monedas.find((x) => x.code === code)
      const sym = row?.symbol ?? code
      return `${sym}${Number(amount).toFixed(2)}`
    },
    [monedas]
  )

  const value = useMemo(
    () => ({
      monedas,
      loading,
      selectedCode,
      setSelectedCode,
      fromUsd,
      formatCatalog,
      formatOrder,
    }),
    [monedas, loading, selectedCode, setSelectedCode, fromUsd, formatCatalog, formatOrder]
  )

  return <CurrencyContext.Provider value={value}>{children}</CurrencyContext.Provider>
}

export function useCurrency() {
  const ctx = useContext(CurrencyContext)
  if (!ctx) throw new Error('useCurrency must be used within CurrencyProvider')
  return ctx
}
