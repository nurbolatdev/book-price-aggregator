import { Link, useLocation } from 'react-router-dom'
import { cn } from '../../lib/cn'
import { focusRing } from '../../components/ui/focusRing'
import { readSearchOrigin } from './searchOrigin'

export function EditionBreadcrumbs({ title }: { title: string }) {
  const origin = readSearchOrigin(useLocation().state)
  const link = cn('hover:text-link', focusRing)
  return (
    <nav aria-label="Хлебные крошки" className="text-sm text-muted">
      <ol className="flex flex-wrap items-center gap-x-2 gap-y-1">
        <li>
          <Link to="/" className={link}>
            Главная
          </Link>
        </li>
        {origin && (
          <>
            <li aria-hidden="true">/</li>
            <li>
              <Link to={origin.href} className={link}>
                {origin.label}
              </Link>
            </li>
          </>
        )}
        <li aria-hidden="true">/</li>
        <li aria-current="page" className="text-ink">
          {title}
        </li>
      </ol>
    </nav>
  )
}
