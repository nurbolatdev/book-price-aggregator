import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'
import { focusRing } from './focusRing'

export type IconButtonVariant = 'outline' | 'plain'

export function iconButtonClasses({
  variant = 'outline',
  className,
}: { variant?: IconButtonVariant; className?: string } = {}): string {
  return cn(
    'inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-ink transition-colors',
    variant === 'outline' ? 'border border-line bg-surface hover:bg-canvas' : 'hover:bg-canvas',
    focusRing,
    className,
  )
}

export interface IconButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'aria-label'> {
  label: string
  variant?: IconButtonVariant
}

export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(function IconButton(
  { label, variant, className, type = 'button', ...rest },
  ref,
) {
  return <button ref={ref} type={type} aria-label={label} className={iconButtonClasses({ variant, className })} {...rest} />
})
