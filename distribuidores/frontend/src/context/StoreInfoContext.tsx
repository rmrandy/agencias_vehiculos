import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

const STORAGE_KEY = 'dist_store_info'

export type StoreInfo = {
  name: string
  subtitle: string
  generalInfo: string
}

const DEFAULT_INFO: StoreInfo = {
  name: 'Distribuidora',
  subtitle: 'Agencias Vehiculos',
  generalInfo: 'Repuestos multiorigen',
}

type StoreInfoContextType = {
  info: StoreInfo
  update: (next: StoreInfo) => void
  reset: () => void
}

const StoreInfoContext = createContext<StoreInfoContextType | null>(null)

function loadStored(): StoreInfo {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return DEFAULT_INFO
    const obj = JSON.parse(raw) as Partial<StoreInfo>
    return {
      name: obj.name?.trim() || DEFAULT_INFO.name,
      subtitle: obj.subtitle?.trim() || DEFAULT_INFO.subtitle,
      generalInfo: obj.generalInfo?.trim() || DEFAULT_INFO.generalInfo,
    }
  } catch {
    return DEFAULT_INFO
  }
}

export function StoreInfoProvider({ children }: { children: ReactNode }) {
  const [info, setInfo] = useState<StoreInfo>(loadStored)

  function update(next: StoreInfo) {
    const clean: StoreInfo = {
      name: next.name.trim() || DEFAULT_INFO.name,
      subtitle: next.subtitle.trim() || DEFAULT_INFO.subtitle,
      generalInfo: next.generalInfo.trim() || DEFAULT_INFO.generalInfo,
    }
    setInfo(clean)
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(clean))
    } catch {
      // ignore storage issues
    }
  }

  function reset() {
    setInfo(DEFAULT_INFO)
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // ignore storage issues
    }
  }

  const value = useMemo(() => ({ info, update, reset }), [info])
  return <StoreInfoContext.Provider value={value}>{children}</StoreInfoContext.Provider>
}

export function useStoreInfo() {
  const ctx = useContext(StoreInfoContext)
  if (!ctx) throw new Error('useStoreInfo must be used within StoreInfoProvider')
  return ctx
}
