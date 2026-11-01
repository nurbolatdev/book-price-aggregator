import { CircleAlert } from 'lucide-react'
import { Alert } from './Alert'
import { Button } from './Button'

interface ErrorBlockProps {
  message?: string
  onRetry: () => void
  className?: string
}

export function ErrorBlock({ message = 'Не удалось загрузить данные.', onRetry, className }: ErrorBlockProps) {
  return (
    <Alert
      tone="danger"
      className={className}
      action={
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Повторить
        </Button>
      }
    >
      <CircleAlert aria-hidden="true" className="h-5 w-5 shrink-0" />
      <span>{message}</span>
    </Alert>
  )
}
