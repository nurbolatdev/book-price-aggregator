import { Suspense } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { useOnline } from '../../lib/useOnline'
import { ErrorBoundary } from '../ErrorBoundary'
import { Alert } from '../ui/Alert'
import { Footer } from './Footer'
import { Header } from './Header'
import { PageSkeleton } from './PageSkeleton'

export function AppLayout() {
  const { pathname } = useLocation()
  const online = useOnline()
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <main className="flex-1">
        {!online && (
          <div className="mx-auto w-full max-w-page px-4 pt-4 md:px-6">
            <Alert tone="offline">Нет подключения к интернету. Показываем сохранённые данные.</Alert>
          </div>
        )}
        <ErrorBoundary
          resetKey={pathname}
          fallback={(_error, reset) => (
            <div className="mx-auto w-full max-w-page px-4 md:px-6 py-10">
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
