import { useEffect, useId, useRef, type ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { StatusIcon, type StatusIconKind } from './StatusIcon'

interface AuthCardProps {
  title: string
  titleSize?: 'form' | 'status'
  description?: ReactNode
  icon?: StatusIconKind
  align?: 'stretch' | 'start'
  focusTitle?: boolean
  children?: ReactNode
}

export function AuthCard({
  title,
  titleSize = 'form',
  description,
  icon,
  align = 'stretch',
  focusTitle = false,
  children,
}: AuthCardProps) {
  const titleId = useId()
  const headingRef = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    if (focusTitle) headingRef.current?.focus()
  }, [focusTitle])

  return (
    <div className="mx-auto w-full max-w-[456px] px-4 py-8 md:py-10">
      <section
        aria-labelledby={titleId}
        className={cn(
          'flex flex-col gap-4 rounded-block border border-line bg-surface p-6 md:p-[30px]',
          align === 'start' && 'items-start',
        )}
      >
        {icon && <StatusIcon kind={icon} />}
        <h1
          id={titleId}
          ref={headingRef}
          tabIndex={focusTitle ? -1 : undefined}
          className={cn(
            'font-display font-medium text-ink outline-none',
            titleSize === 'form' ? 'text-2xl' : 'text-[22px]',
          )}
        >
          {title}
        </h1>
        {description && <p className="leading-normal text-muted">{description}</p>}
        {children}
      </section>
    </div>
  )
}
