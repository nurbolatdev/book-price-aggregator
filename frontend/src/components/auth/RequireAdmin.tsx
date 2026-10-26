import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../stores/auth'
import { PageSkeleton } from '../layout/PageSkeleton'

export function RequireAdmin({ children }: { children?: ReactNode }) {
  const status = useAuthStore((s) => s.status)
  const role = useAuthStore((s) => s.user?.role)
  const { pathname, search } = useLocation()

  if (status === 'restoring') return <PageSkeleton />
  if (status === 'anonymous') {
    return <Navigate to={`/login?next=${encodeURIComponent(pathname + search)}`} replace />
  }
  if (role !== 'ADMIN') return <Navigate to="/" replace />
  return children ?? <Outlet />
}
