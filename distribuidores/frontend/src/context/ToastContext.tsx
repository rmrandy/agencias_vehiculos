import { createContext, useContext, useState, useCallback, ReactNode } from 'react'
import { Toast } from '../components/Toast'

export type ToastType = 'success' | 'error' | 'info'

interface ToastItem {
  id: number
  message: string
  type: ToastType
}

type ToastContextType = {
  toast: (message: string, type?: ToastType) => void
  success: (message: string) => void
  error: (message: string) => void
  info: (message: string) => void
}

const ToastContext = createContext<ToastContextType | null>(null)

let id = 0

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const add = useCallback((message: string, type: ToastType = 'info') => {
    const currentId = ++id
    setItems((prev) => [...prev, { id: currentId, message, type }])
    setTimeout(() => {
      setItems((prev) => prev.filter((t) => t.id !== currentId))
    }, 3500)
  }, [])

  const toast = useCallback((message: string, type?: ToastType) => add(message, type ?? 'info'), [add])
  const success = useCallback((message: string) => add(message, 'success'), [add])
  const error = useCallback((message: string) => add(message, 'error'), [add])
  const info = useCallback((message: string) => add(message, 'info'), [add])

  return (
    <ToastContext.Provider value={{ toast, success, error, info }}>
      {children}
      <div className="toast-container" aria-live="polite">
        {items.map((t) => (
          <Toast key={t.id} message={t.message} type={t.type} onClose={() => setItems((prev) => prev.filter((x) => x.id !== t.id))} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
