import { Button } from '../../components/ui/Button'
import { Modal } from '../../components/ui/Modal'

interface ConfirmDialogProps {
  open: boolean
  title: string
  text: string
  confirmLabel: string
  tone: 'danger' | 'dark'
  pending?: boolean
  onConfirm: () => void
  onClose: () => void
}

export function ConfirmDialog({ open, title, text, confirmLabel, tone, pending, onConfirm, onClose }: ConfirmDialogProps) {
  return (
    <Modal open={open} onClose={onClose} title={title}>
      <p className="text-[15px] leading-normal text-ink-soft">{text}</p>
      <div className="flex justify-end gap-2">
        <Button variant="secondary" size="sm" onClick={onClose}>
          Отмена
        </Button>
        <Button variant={tone} size="sm" loading={pending} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  )
}
