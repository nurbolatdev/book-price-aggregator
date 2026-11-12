import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { Loader2 } from 'lucide-react'
import { cn } from '../../lib/cn'
import { focusRing } from './focusRing'

export type ButtonVariant = 'primary' | 'secondary' | 'light' | 'dark' | 'ghost' | 'outline-accent'
export type ButtonSize = 'sm' | 'md' | 'lg' | 'xl' | 'bar' | 'form'

const variants: Record<ButtonVariant, string> = {
  primary: 'bg-accent text-white hover:bg-accent/90',
  secondary: 'border border-line bg-surface text-ink hover:bg-canvas',
  light: 'bg-surface text-ink hover:bg-canvas',
  dark: 'bg-ink text-white hover:bg-ink-soft',
  ghost: 'bg-transparent text-ink hover:bg-canvas',
  'outline-accent': 'border-2 border-accent bg-surface text-accent hover:bg-accent-soft',
}

const sizes: Record<ButtonSize, string> = {
  sm: 'h-10 px-4 text-sm',
  md: 'h-11 px-5 text-[15px]',
  lg: 'h-12 px-6 text-base',
  xl: 'h-[52px] px-7 text-base',
  bar: 'h-[52px] px-2.5 text-base',
  form: 'h-12 px-6 text-base',
}

interface ButtonClassOptions {
  variant?: ButtonVariant
  size?: ButtonSize
  className?: string
}

export function buttonClasses({ variant = 'primary', size = 'md', className }: ButtonClassOptions = {}): string {
  return cn(
    'inline-flex items-center justify-center gap-2 rounded-xl transition-colors',
    size === 'bar' || size === 'form' ? 'font-bold' : 'font-semibold',
    'disabled:cursor-not-allowed disabled:opacity-60',
    variants[variant],
    sizes[size],
    focusRing,
    className,
  )
}

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>, ButtonClassOptions {
  loading?: boolean
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant, size, className, loading = false, disabled, onClick, children, type = 'button', ...rest },
  ref,
) {
  return (
    <button
      ref={ref}
      type={type}
      className={buttonClasses({ variant, size, className })}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      onClick={loading ? undefined : onClick}
      {...rest}
    >
      {loading && <Loader2 aria-hidden="true" className="h-4 w-4 animate-spin" />}
      {children}
    </button>
  )
})
