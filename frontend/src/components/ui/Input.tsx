import { forwardRef, type InputHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'
import { focusRing } from './focusRing'

export const inputClasses =
  'h-11 w-full rounded-xl border border-line bg-surface px-3.5 text-[15px] text-ink placeholder:text-muted disabled:opacity-60 aria-[invalid=true]:border-danger'

const largeInputClasses =
  'h-12 w-full rounded-control border border-line bg-surface px-3.5 text-base text-ink placeholder:text-muted disabled:opacity-60 aria-[invalid=true]:border-2 aria-[invalid=true]:border-danger aria-[invalid=true]:px-[13px]'

export type InputSize = 'md' | 'lg'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  inputSize?: InputSize
  trailingButton?: boolean
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className, inputSize = 'md', trailingButton = false, ...rest },
  ref,
) {
  return (
    <input
      ref={ref}
      className={cn(
        inputSize === 'lg' ? largeInputClasses : inputClasses,
        trailingButton && 'pr-12 aria-[invalid=true]:pr-12',
        focusRing,
        className,
      )}
      {...rest}
    />
  )
})
