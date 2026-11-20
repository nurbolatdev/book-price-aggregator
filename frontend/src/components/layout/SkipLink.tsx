import type { MouseEvent } from 'react'
import { cn } from '../../lib/cn'
import { focusRing } from '../ui/focusRing'

export function SkipLink({ target = 'main' }: { target?: string }) {
  const onClick = (e: MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault()
    document.getElementById(target)?.focus()
  }
  return (
    <a
      href={`#${target}`}
      onClick={onClick}
      className={cn(
        'sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 rounded-control bg-surface px-4 py-3 font-semibold text-ink shadow-lg',
        focusRing,
      )}
    >
      Перейти к содержимому
    </a>
  )
}
