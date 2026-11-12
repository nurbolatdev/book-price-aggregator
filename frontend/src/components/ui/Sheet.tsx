import { useId, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { cn } from '../../lib/cn'
import { IconButton } from './IconButton'
import { useDialogFocus } from './useDialogFocus'

interface SheetProps {
  open: boolean
  onClose: () => void
  title: string
  side?: 'bottom' | 'right'
  children: ReactNode
}

export function Sheet({ open, onClose, title, side = 'bottom', children }: SheetProps) {
  const titleId = useId()
  const panelRef = useRef<HTMLDivElement>(null)
  const onPanelKeyDown = useDialogFocus(open, panelRef, onClose)

  if (!open) return null

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
          'absolute flex min-h-0 flex-col overflow-hidden bg-surface shadow-menu',
          side === 'bottom'
            ? 'inset-x-0 bottom-0 max-h-[min(760px,calc(100dvh-48px))] rounded-t-block'
            : 'inset-y-0 right-0 h-full w-72',
        )}
      >
        <div className="flex items-center justify-between gap-2 py-2 pl-4 pr-2">
          <h2 id={titleId} className="font-sans text-[17px] font-bold text-ink">
            {title}
          </h2>
          <IconButton label="Закрыть" variant="plain" onClick={onClose}>
            <X aria-hidden="true" className="h-5 w-5" />
          </IconButton>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  )
}
