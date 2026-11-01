import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'
import { focusRing } from './focusRing'

export function chipClasses({ selected = false, className }: { selected?: boolean; className?: string } = {}): string {
  return cn(
    'inline-flex min-h-touch items-center gap-1.5 rounded-full border px-4 text-sm transition-colors',
    selected ? 'border-ink bg-ink text-white' : 'border-line bg-surface text-ink hover:bg-canvas',
    focusRing,
    className,
  )
}

export interface ChipProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  selected?: boolean
  count?: number
}

export const Chip = forwardRef<HTMLButtonElement, ChipProps>(function Chip(
  { selected = false, count, className, children, type = 'button', ...rest },
  ref,
) {
  return (
    <button ref={ref} type={type} aria-pressed={selected} className={chipClasses({ selected, className })} {...rest}>
      {children}
      {count !== undefined && <span className={selected ? 'text-mist' : 'text-muted'}>{count}</span>}
    </button>
  )
})
