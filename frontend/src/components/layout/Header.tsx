import { Link } from 'react-router-dom'
import logo from '../../assets/logo-full.svg'
import { useAuthStore } from '../../stores/auth'

const navLink =
  'inline-flex min-h-touch items-center rounded-control px-3 text-sm font-medium text-ink hover:text-link focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-link'

export function Header() {
  const status = useAuthStore((s) => s.status)
  const user = useAuthStore((s) => s.user)

  return (
    <header className="border-b border-line bg-surface">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-4 py-2">
        <Link to="/" className={navLink + ' -ml-3'}>
          <img src={logo} alt="KitapRadar" className="h-8 w-auto" />
        </Link>
        <nav className="flex flex-wrap items-center gap-1">
          {status === 'authenticated' && user ? (
            <>
              <Link to="/beacons" className={navLink}>
                Маячки
              </Link>
              {user.role === 'ADMIN' && (
                <Link to="/admin" className={navLink}>
                  Админка
                </Link>
              )}
              <Link to="/profile" className={navLink}>
                {user.name}
              </Link>
            </>
          ) : (
            status === 'anonymous' && (
              <Link to="/login" className={navLink}>
                Войти
              </Link>
            )
          )}
        </nav>
      </div>
    </header>
  )
}
