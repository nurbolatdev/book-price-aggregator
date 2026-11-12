import { Navigate, Outlet, useSearchParams } from 'react-router-dom'
import { safeNext } from '../../features/auth/safeNext'
import { useAuthStore } from '../../stores/auth'
import { PageSkeleton } from '../layout/PageSkeleton'

export function RequireGuest() {
  const status = useAuthStore((s) => s.status)
  const [params] = useSearchParams()

  if (status === 'restoring') return <PageSkeleton />
  if (status === 'authenticated') return <Navigate replace to={safeNext(params.get('next'))} />
  return <Outlet />
}
