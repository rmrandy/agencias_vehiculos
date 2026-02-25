import './LoadingModal.css'

interface LoadingModalProps {
  open: boolean
  message?: string
}

export function LoadingModal({ open, message = 'Cargando...' }: LoadingModalProps) {
  if (!open) return null
  return (
    <div className="loading-modal-overlay" role="dialog" aria-busy="true" aria-label={message}>
      <div className="loading-modal">
        <div className="loading-modal-spinner" />
        <p className="loading-modal-text">{message}</p>
      </div>
    </div>
  )
}
