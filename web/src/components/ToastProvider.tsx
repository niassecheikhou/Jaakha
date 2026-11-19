import {
  useCallback,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { ToastContext, type ToastContextValue, type ToastType } from './toast-context'

type Toast = {
  id: number
  message: string
  title: string
  type: ToastType
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const removeToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const showToast = useCallback((type: ToastType, title: string, message: string) => {
    const id = Date.now() + Math.random()
    setToasts((current) => [...current, { id, message, title, type }])
    window.setTimeout(() => removeToast(id), 5200)
  }, [removeToast])

  const value = useMemo<ToastContextValue>(() => ({
    error: (title, message) => showToast('error', title, message),
    success: (title, message) => showToast('success', title, message),
  }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-viewport" aria-live="polite" aria-atomic="true">
        {toasts.map((toast) => (
          <article className={`toast toast-${toast.type}`} key={toast.id}>
            <div>
              <strong>{toast.title}</strong>
              <p>{toast.message}</p>
            </div>
            <button aria-label="Fermer la notification" type="button" onClick={() => removeToast(toast.id)}>
              <CloseIcon />
            </button>
          </article>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

function CloseIcon() {
  return (
    <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
      <path d="M18 6 6 18M6 6l12 12" />
    </svg>
  )
}
