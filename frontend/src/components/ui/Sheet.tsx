import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { cn } from '../../lib/cn'

interface SheetProps {
  open: boolean
  onClose: () => void
  title: string
  side?: 'bottom' | 'right'
  children: ReactNode
}

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

export function Sheet({ open, onClose, title, side = 'bottom', children }: SheetProps) {
  const titleId = useId()
  const panelRef = useRef<HTMLDivElement>(null)
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    if (!open) return
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const first = panelRef.current?.querySelector<HTMLElement>(FOCUSABLE)
    ;(first ?? panelRef.current)?.focus()

    function onKey(event: globalThis.KeyboardEvent) {
      if (event.key === 'Escape') {
        event.stopPropagation()
        onCloseRef.current()
      }
    }
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = overflow
      previous?.focus()
    }
  }, [open])

  if (!open) return null

  function onPanelKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key !== 'Tab') return
    const items = Array.from(panelRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? [])
    if (items.length === 0) {
      event.preventDefault()
      return
    }
    const firstItem = items[0]
    const lastItem = items[items.length - 1]
    if (event.shiftKey && document.activeElement === firstItem) {
      event.preventDefault()
      lastItem?.focus()
    } else if (!event.shiftKey && document.activeElement === lastItem) {
      event.preventDefault()
      firstItem?.focus()
    }
  }

  return createPortal(
    <div className="fixed inset-0 z-50">
      <div data-testid="sheet-overlay" className="absolute inset-0 bg-ink/45" onClick={onClose} />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onKeyDown={onPanelKeyDown}
        className={cn(
          'absolute flex flex-col overflow-y-auto bg-surface shadow-menu',
          side === 'bottom'
            ? 'inset-x-0 bottom-0 max-h-[760px] rounded-t-block'
            : 'inset-y-0 right-0 h-full w-72',
        )}
      >
        <h2 id={titleId} className="px-4 pb-2 pt-4 font-display text-lg font-medium text-ink">
          {title}
        </h2>
        {children}
      </div>
    </div>,
    document.body,
  )
}
