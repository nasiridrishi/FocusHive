/**
 * Simple Toast Hook
 * 
 * Provides toast notifications to replace alert() calls
 * This is a basic implementation that can be enhanced with a UI library later
 */

import { useCallback } from 'react'

type ToastType = 'success' | 'error' | 'warning' | 'info'

interface ToastOptions {
  type?: ToastType
  duration?: number
  position?: 'top' | 'bottom'
}

// Simple toast implementation using browser notifications as fallback
// In a real app, this would integrate with a toast library like react-hot-toast
export const useToast = (): {
  showToast: (message: string, options?: ToastOptions) => void;
  success: (message: string, options?: Omit<ToastOptions, 'type'>) => void;
  error: (message: string, options?: Omit<ToastOptions, 'type'>) => void;
  warning: (message: string, options?: Omit<ToastOptions, 'type'>) => void;
  info: (message: string, options?: Omit<ToastOptions, 'type'>) => void;
} => {
  const showToast = useCallback((message: string, options: ToastOptions = {}) => {
    const { type = 'info', duration = 3000 } = options

    // For now, we'll use a combination of console and temporary DOM element
    // This is a basic implementation to replace alert() statements
    // console.info(`[${type.toUpperCase()}] ${message}`)

    // Create a simple toast element
    const toast = document.createElement('div')
    toast.textContent = message
    toast.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      background: ${getBackgroundColor(type)};
      color: white;
      padding: 12px 16px;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      z-index: 10000;
      font-family: system-ui, -apple-system, sans-serif;
      font-size: 14px;
      max-width: 300px;
      word-wrap: break-word;
      transition: all 0.3s ease;
    `

    document.body.appendChild(toast)

    // Animate in
    setTimeout(() => {
      toast.style.transform = 'translateX(0)'
      toast.style.opacity = '1'
    }, 10)

    // Remove after duration
    setTimeout(() => {
      toast.style.transform = 'translateX(100%)'
      toast.style.opacity = '0'
      setTimeout(() => {
        if (document.body.contains(toast)) {
          document.body.removeChild(toast)
        }
      }, 300)
    }, duration)
  }, [])

  const success = useCallback((message: string, options?: Omit<ToastOptions, 'type'>) => {
    showToast(message, { ...options, type: 'success' })
  }, [showToast])

  const error = useCallback((message: string, options?: Omit<ToastOptions, 'type'>) => {
    showToast(message, { ...options, type: 'error' })
  }, [showToast])

  const warning = useCallback((message: string, options?: Omit<ToastOptions, 'type'>) => {
    showToast(message, { ...options, type: 'warning' })
  }, [showToast])

  const info = useCallback((message: string, options?: Omit<ToastOptions, 'type'>) => {
    showToast(message, { ...options, type: 'info' })
  }, [showToast])

  return {
    showToast,
    success,
    error,
    warning,
    info
  }
}

const getBackgroundColor = (type: ToastType): string => {
  switch (type) {
    case 'success': return '#4caf50'
    case 'error': return '#f44336'
    case 'warning': return '#ff9800'
    case 'info': return '#2196f3'
    default: return '#2196f3'
  }
}

export default useToast