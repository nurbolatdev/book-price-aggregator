import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'
import { focusRing } from './focusRing'

export type ChipSize = 'md' | 'sm'

export function chipClasses({
  selected = false,
  size = 'md',
  className,
}: { selected?: boolean; size?: ChipSize; className?: string } = {}): string {
  return cn(
    'inline-flex items-center gap-1.5 rounded-full border text-sm transition-colors',
    size === 'sm' ? 'px-3 py-1.5' : 'min-h-touch px-4',
    selected ? 'border-ink bg-ink text-white' : 'border-line bg-surface text-ink hover:bg-canvas',
    focusRing,
    className,
  )
}

export interface ChipProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  selected?: boolean
  size?: ChipSize
  count?: number
}

export const Chip = forwardRef<HTMLButtonElement, ChipProps>(function Chip(
  { selected = false, size, count, className, children, type = 'button', ...rest },
  ref,
) {
  return (
    <button ref={ref} type={type} aria-pressed={selected} className={chipClasses({ selected, size, className })} {...rest}>
      {children}
      {count !== undefined && <span className={selected ? 'text-mist' : 'text-muted'}>{count}</span>}
    </button>
  )
})
