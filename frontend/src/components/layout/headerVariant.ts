export type HeaderVariant = 'nav' | 'search'

export function getHeaderVariant(pathname: string): HeaderVariant {
  if (pathname === '/search' || pathname === '/catalog') return 'search'
  if (pathname === '/editions' || pathname.startsWith('/editions/')) return 'search'
  return 'nav'
}
