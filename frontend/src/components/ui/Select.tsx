import { forwardRef, type SelectHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'
import { focusRing } from './focusRing'

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(function Select(
  { className, ...rest },
  ref,
) {
  return (
    <select
      ref={ref}
      className={cn(
        'h-11 rounded-xl border border-line bg-surface px-3 text-[15px] text-ink disabled:opacity-60 aria-[invalid=true]:border-danger',
        focusRing,
        className,
      )}
      {...rest}
    />
  )
})
