import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'
import { focusRing } from './focusRing'

export type IconButtonVariant = 'outline' | 'plain'
export type IconButtonTone = 'ink' | 'accent'

export function iconButtonClasses({
  variant = 'outline',
  tone = 'ink',
  className,
}: { variant?: IconButtonVariant; tone?: IconButtonTone; className?: string } = {}): string {
  return cn(
    'inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-xl transition-colors',
    tone === 'accent' ? 'text-accent' : 'text-ink',
    variant === 'outline' ? 'border border-line bg-surface hover:bg-canvas' : 'hover:bg-canvas',
    focusRing,
    className,
  )
}

export interface IconButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'aria-label'> {
  label: string
  variant?: IconButtonVariant
  tone?: IconButtonTone
}

export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(function IconButton(
  { label, variant, tone, className, type = 'button', ...rest },
  ref,
) {
  return <button ref={ref} type={type} aria-label={label} className={iconButtonClasses({ variant, tone, className })} {...rest} />
})
