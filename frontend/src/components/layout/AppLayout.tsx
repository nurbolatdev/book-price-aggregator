import { Suspense } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { ErrorBoundary } from '../ErrorBoundary'
import { Footer } from './Footer'
import { Header } from './Header'
import { PageSkeleton } from './PageSkeleton'

export function AppLayout() {
  const { pathname } = useLocation()
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex-1">
        <ErrorBoundary
          resetKey={pathname}
          fallback={(_error, reset) => (
            <div className="mx-auto max-w-6xl px-4 py-10">
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
          <Suspense fallback={<PageSkeleton />}>
            <Outlet />
          </Suspense>
        </ErrorBoundary>
      </main>
      <Footer />
    </div>
  )
}
