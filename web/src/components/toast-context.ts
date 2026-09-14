import { createContext, useContext } from 'react'

export type ToastType = 'success' | 'error'

export type ToastContextValue = {
  error: (title: string, message: string) => void
  success: (title: string, message: string) => void
}

export const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used inside ToastProvider')
  }
  return context
}
