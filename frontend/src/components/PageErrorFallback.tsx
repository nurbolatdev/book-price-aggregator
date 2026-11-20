import { Button } from './ui/Button'

export function PageErrorFallback({ onRetry }: { onRetry: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-start gap-2 rounded-xl border border-dashed border-line p-[18px]">
      <strong>Что-то пошло не так</strong>
      <span className="text-sm text-muted">Ошибка на странице. Попробуйте обновить её.</span>
      <Button variant="secondary" size="sm" onClick={onRetry}>
        Обновить страницу
      </Button>
    </div>
  )
}
