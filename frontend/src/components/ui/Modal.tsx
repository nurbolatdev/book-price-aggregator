import { useId, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { useMediaQuery } from '../../lib/useMediaQuery'
import { IconButton } from './IconButton'
import { Sheet } from './Sheet'
import { useDialogFocus } from './useDialogFocus'

interface ModalProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
}

export function Modal({ open, onClose, title, children }: ModalProps) {
  const isDesktop = useMediaQuery('(min-width: 768px)')
  if (!isDesktop) {
    return (
      <Sheet open={open} onClose={onClose} title={title} side="bottom">
        <div className="min-h-0 overflow-y-auto px-4 pb-5">{children}</div>
      </Sheet>
    )
  }
  return (
    <ModalDialog open={open} onClose={onClose} title={title}>
      {children}
    </ModalDialog>
  )
}

function ModalDialog({ open, onClose, title, children }: ModalProps) {
  const titleId = useId()
  const panelRef = useRef<HTMLDivElement>(null)
  const onPanelKeyDown = useDialogFocus(open, panelRef, onClose)

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div data-testid="modal-overlay" className="absolute inset-0 bg-ink/45" onClick={onClose} />
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onKeyDown={onPanelKeyDown}
        className="relative flex max-h-full w-full max-w-[560px] flex-col gap-4 overflow-y-auto rounded-block bg-surface p-6 shadow-menu"
      >
        <div className="flex items-start justify-between gap-3">
          <h2 id={titleId} className="font-display text-xl font-medium text-ink">
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
