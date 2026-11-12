export type HeaderVariant = 'nav' | 'search' | 'minimal'

const MINIMAL_PATHS = new Set([
  '/login',
  '/register',
  '/check-email',
  '/verify-email',
  '/forgot-password',
  '/reset-password',
])

export function getHeaderVariant(pathname: string): HeaderVariant {
  if (MINIMAL_PATHS.has(pathname)) return 'minimal'
  if (pathname === '/search' || pathname === '/catalog') return 'search'
  if (pathname === '/editions' || pathname.startsWith('/editions/')) return 'search'
  return 'nav'
}

export function hasOwnPhoneHeader(pathname: string): boolean {
  return pathname.startsWith('/editions/')
}
