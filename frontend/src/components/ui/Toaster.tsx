import { useToastStore, type ToastKind } from '../../stores/toast'

const STYLES: Record<ToastKind, string> = {
  success: 'border-deal bg-deal-soft text-ink',
  error: 'border-accent bg-surface text-accent',
  info: 'border-line bg-surface text-ink',
}

export function Toaster() {
  const toasts = useToastStore((s) => s.toasts)
  const dismiss = useToastStore((s) => s.dismiss)

  return (
    <div
      aria-live="polite"
      role="status"
      className="fixed bottom-4 right-4 z-50 flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2"
    >
      {toasts.map((t) => (
        <div
          key={t.id}
          data-kind={t.kind}
          className={`flex items-center justify-between gap-2 rounded-control border px-4 py-2 shadow ${STYLES[t.kind]}`}
        >
          <span>{t.message}</span>
          <button
            type="button"
            aria-label="Закрыть"
            onClick={() => dismiss(t.id)}
            className="min-h-touch min-w-touch rounded-control focus:outline-none focus-visible:ring-2 focus-visible:ring-link"
          >
            ×
          </button>
        </div>
      ))}
    </div>
  )
}
