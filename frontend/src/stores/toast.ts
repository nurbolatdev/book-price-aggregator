import { create } from 'zustand'

export type ToastKind = 'success' | 'error' | 'info'
export type ToastCloseReason = 'timeout' | 'dismiss' | 'action'

export interface ToastAction {
  label: string
  onClick: () => void
}

export interface ToastOptions {
  action?: ToastAction
  duration?: number
  onClose?: (reason: ToastCloseReason) => void
}

export interface ToastItem {
  id: number
  kind: ToastKind
  message: string
  action?: ToastAction
}

interface ToastState {
  toasts: ToastItem[]
  push: (kind: ToastKind, message: string, options?: ToastOptions) => number
  dismiss: (id: number, reason?: ToastCloseReason) => void
  remove: (id: number) => void
}

const HIDE_AFTER_MS: Record<ToastKind, number> = { success: 5000, info: 5000, error: 7000 }

let nextId = 1
const timers = new Map<number, ReturnType<typeof setTimeout>>()
const closeHandlers = new Map<number, (reason: ToastCloseReason) => void>()

function clearTimer(id: number) {
  const timer = timers.get(id)
  if (timer !== undefined) clearTimeout(timer)
  timers.delete(id)
}

export const useToastStore = create<ToastState>((set, get) => ({
  toasts: [],
  push: (kind, message, options = {}) => {
    const id = nextId++
    set((state) => ({ toasts: [...state.toasts, { id, kind, message, action: options.action }] }))
    if (options.onClose) closeHandlers.set(id, options.onClose)
    timers.set(
      id,
      setTimeout(() => get().dismiss(id, 'timeout'), options.duration ?? HIDE_AFTER_MS[kind]),
    )
    return id
  },
  dismiss: (id, reason = 'dismiss') => {
    clearTimer(id)
    const onClose = closeHandlers.get(id)
    closeHandlers.delete(id)
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }))
    onClose?.(reason)
  },
  remove: (id) => {
    clearTimer(id)
    closeHandlers.delete(id)
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }))
  },
}))

export const toast = {
  success: (message: string, options?: ToastOptions) => useToastStore.getState().push('success', message, options),
  error: (message: string, options?: ToastOptions) => useToastStore.getState().push('error', message, options),
  info: (message: string, options?: ToastOptions) => useToastStore.getState().push('info', message, options),
}
