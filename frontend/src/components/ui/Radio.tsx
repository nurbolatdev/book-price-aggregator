import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'
import { focusRing } from './focusRing'

export interface RadioProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string
  count?: number
}

export const Radio = forwardRef<HTMLInputElement, RadioProps>(function Radio(
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
      <input ref={ref} type="radio" disabled={disabled} className={cn('h-5 w-5 shrink-0 accent-ink', focusRing)} {...rest} />
      <span>{label}</span>
      {count !== undefined && <span className="ml-auto text-sm text-muted">{count}</span>}
    </label>
  )
})
