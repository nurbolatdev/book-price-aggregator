import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'
import { focusRing } from './focusRing'

export interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string
  count?: number
}

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(function Checkbox(
  { label, count, className, disabled, ...rest },
  ref,
) {
  return (
    <label
      className={cn(
        'flex min-h-touch cursor-pointer items-center gap-2.5 text-[15px] text-ink',
        disabled && 'cursor-not-allowed opacity-50',
        className,
      )}
    >
      <input ref={ref} type="checkbox" disabled={disabled} className={cn('h-5 w-5 shrink-0 accent-ink', focusRing)} {...rest} />
      <span>{label}</span>
      {count !== undefined && <span className="ml-auto text-sm text-muted">{count}</span>}
    </label>
  )
})
