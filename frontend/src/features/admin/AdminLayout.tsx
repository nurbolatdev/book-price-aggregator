import { Suspense } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { ErrorBoundary } from '../../components/ErrorBoundary'
import { ScrollToTop } from '../../components/layout/ScrollToTop'
import { Alert } from '../../components/ui/Alert'
import { focusRing } from '../../components/ui/focusRing'
import { Skeleton } from '../../components/ui/Skeleton'
import { cn } from '../../lib/cn'
import { useMediaQuery } from '../../lib/useMediaQuery'

const links = [
  { to: '/admin', label: 'Обзор', end: true },
  { to: '/admin/parsers', label: 'Парсеры' },
  { to: '/admin/queues', label: 'Очереди RabbitMQ' },
  { to: '/admin/users', label: 'Пользователи' },
  { to: '/admin/notifications', label: 'Маячки и письма' },
  { to: '/admin/matching', label: 'Каталог и сопоставление' },
  { to: '/admin/events', label: 'Журнал событий' },
]

export function AdminLayout() {
  const { pathname } = useLocation()
  const wide = useMediaQuery('(min-width: 1024px)')
  return (
    <div className="min-h-screen overflow-x-auto bg-canvas text-ink">
      <ScrollToTop />
      <div className="flex min-h-screen min-w-[1024px]">
        <nav aria-label="Админ-меню" className="flex w-[240px] shrink-0 flex-col gap-1 bg-ink px-3.5 py-5 text-white">
          <div className="flex items-center gap-2.5 px-2 pb-[18px]">
            <svg
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              aria-hidden="true"
              className="text-admin-logo"
            >
              <circle cx="12" cy="12" r="10" />
              <circle cx="12" cy="12" r="6" />
              <circle cx="12" cy="12" r="2" />
              <line x1="12" y1="12" x2="19" y2="5" />
            </svg>
            <span className="font-display text-[15px] font-bold">Admin</span>
          </div>
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.end}
              className={({ isActive }) =>
                cn(
                  'rounded-control px-3 py-2.5',
                  isActive ? 'bg-admin-nav font-semibold text-white' : 'text-mist hover:text-white',
                  focusRing,
                )
              }
            >
              {l.label}
            </NavLink>
          ))}
          <Link to="/" className={cn('mt-4 rounded-control px-3 py-2.5 text-sm text-mist hover:text-white', focusRing)}>
            ← На сайт
          </Link>
        </nav>
        <main className="flex min-w-0 flex-1 flex-col gap-[22px] p-7">
          {!wide && <Alert tone="info">Админ-панель рассчитана на экран от 1024 px</Alert>}
          <ErrorBoundary
            resetKey={pathname}
            fallback={(_error, reset) => (
              <div>
                <p className="mb-4">Не удалось показать страницу</p>
                <button
                  type="button"
                  onClick={reset}
                  className="min-h-touch rounded-control border border-line bg-surface px-4 focus:outline-none focus-visible:ring-2 focus-visible:ring-link"
                >
                  Попробовать снова
                </button>
              </div>
            )}
          >
            <Suspense fallback={<Skeleton className="h-64 w-full" />}>
              <Outlet />
            </Suspense>
          </ErrorBoundary>
        </main>
      </div>
    </div>
  )
}
