import './Toast.css'

interface ToastProps {
  message: string
  type: 'success' | 'error' | 'info'
  onClose?: () => void
}

export function Toast({ message, type, onClose }: ToastProps) {
  return (
    <div className={`toast toast-${type}`} role="alert">
      <span className="toast-message">{message}</span>
      {onClose && (
        <button type="button" className="toast-close" onClick={onClose} aria-label="Cerrar">
          ×
        </button>
      )}
    </div>
  )
}
