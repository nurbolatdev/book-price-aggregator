import { Suspense, type ReactNode } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { ErrorBoundary } from '../ErrorBoundary'
import { PageErrorFallback } from '../PageErrorFallback'
import { Footer } from './Footer'
import { Header } from './Header'
import { OfflineBanner } from './OfflineBanner'
import { PageSkeleton } from './PageSkeleton'
import { ScrollToTop } from './ScrollToTop'

export function AppLayout({ children }: { children?: ReactNode }) {
  const { pathname } = useLocation()
  return (
    <div className="flex min-h-screen flex-col">
      <ScrollToTop />
      <Header />
      <main className="flex-1">
        <div className="mx-auto w-full max-w-page px-4 pt-4 empty:hidden md:px-6">
          <OfflineBanner />
        </div>
        <ErrorBoundary
          resetKey={pathname}
          fallback={(_error, reset) => (
            <div className="mx-auto w-full max-w-page px-4 py-10 md:px-6">
              <PageErrorFallback onRetry={reset} />
            </div>
          )}
        >
          <Suspense fallback={<PageSkeleton />}>
            {children ?? <Outlet />}
          </Suspense>
        </ErrorBoundary>
      </main>
      <Footer />
    </div>
  )
}
