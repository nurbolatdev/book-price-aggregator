import { useLayoutEffect, useRef, useState } from 'react'
import { cn } from '../../lib/cn'
import { focusRing } from '../../components/ui/focusRing'

export function EditionDescription({ text }: { text?: string | null }) {
  const ref = useRef<HTMLParagraphElement>(null)
  const [open, setOpen] = useState(false)
  const [overflows, setOverflows] = useState(false)

  useLayoutEffect(() => {
    const el = ref.current
    if (el && !open) setOverflows(el.scrollHeight > el.clientHeight + 1)
  }, [text, open])

  if (!text) return null
  return (
    <div className="flex flex-col items-start gap-1.5">
      <p ref={ref} className={cn('leading-[1.6] text-ink-soft', !open && 'line-clamp-4')}>
        {text}
      </p>
      {(overflows || open) && (
        <button
          type="button"
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
          className={cn('min-h-touch font-semibold text-link hover:underline', focusRing)}
        >
          {open ? 'Свернуть' : 'Читать полностью'}
        </button>
      )}
    </div>
  )
}
