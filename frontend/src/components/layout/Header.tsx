import { useState, type FormEvent } from 'react'
import { Bell, Menu, Search } from 'lucide-react'
import { Link, NavLink, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import logo from '../../assets/logo-full.svg'
import { cn } from '../../lib/cn'
import { useAuthStore } from '../../stores/auth'
import { focusRing } from '../ui/focusRing'
import { IconButton, iconButtonClasses } from '../ui/IconButton'
import { getHeaderVariant } from './headerVariant'
import { MobileMenu } from './MobileMenu'
import { UserMenu } from './UserMenu'

const navLinkClasses = ({ isActive }: { isActive: boolean }) =>
  cn(
    'inline-flex min-h-touch items-center rounded-control text-[15px] font-medium hover:text-accent',
    isActive ? 'text-accent' : 'text-ink',
    focusRing,
  )

const BEACONS_LOGIN = '/login?next=%2Fbeacons'

function HeaderSearch({ defaultValue }: { defaultValue: string }) {
  const navigate = useNavigate()
  const [value, setValue] = useState(defaultValue)

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmed = value.trim()
    if (trimmed.length < 2) return
    navigate('/search?q=' + encodeURIComponent(trimmed))
  }

  return (
    <form role="search" onSubmit={onSubmit} className="relative flex-[1_1_360px] max-md:order-last max-md:basis-full">
      <Search
        aria-hidden="true"
        size={20}
        className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-muted"
      />
      <input
        type="search"
        aria-label="Поиск книги"
        placeholder="Название, автор или ISBN"
        value={value}
        onChange={(event) => setValue(event.target.value)}
        className={cn(
          'h-[46px] w-full rounded-xl border border-line bg-canvas pl-11 pr-3.5 text-[15px] text-ink placeholder:text-muted',
          focusRing,
        )}
      />
    </form>
  )
}

export function Header() {
  const status = useAuthStore((s) => s.status)
  const [menuOpen, setMenuOpen] = useState(false)
  const { pathname } = useLocation()
  const [params] = useSearchParams()
  const variant = getHeaderVariant(pathname)
  const authenticated = status === 'authenticated'
  const q = params.get('q') ?? ''
  const beaconsHref = authenticated ? '/beacons' : BEACONS_LOGIN

  const account = authenticated ? (
    <UserMenu />
  ) : (
    status === 'anonymous' && (
      <Link
        to="/login"
        className={cn(
          'inline-flex h-10 items-center rounded-xl border border-line px-4 text-sm font-semibold text-ink hover:bg-canvas md:h-auto md:text-[15px]',
          variant === 'nav' ? 'md:px-[18px] md:py-2.5' : 'md:px-4 md:py-[9px]',
          focusRing,
        )}
      >
        Войти
      </Link>
    )
  )

  return (
    <header className="border-b border-line bg-surface">
      <div
        className={cn(
          'mx-auto flex w-full max-w-page flex-wrap items-center gap-2 px-3 py-2.5 md:px-6',
          variant === 'nav' ? 'md:gap-6 md:py-3.5' : 'md:gap-5 md:py-3',
        )}
      >
        <Link to="/" className={cn('inline-flex shrink-0 items-center rounded-control', focusRing)}>
          <img src={logo} alt="KitapRadar" className={variant === 'nav' ? 'h-[26px] w-auto md:h-7' : 'h-[26px] w-auto'} />
        </Link>
        {variant === 'nav' ? (
          <nav aria-label="Основная навигация" className="hidden flex-grow items-center gap-5 md:flex">
            <NavLink to="/catalog" className={navLinkClasses}>
              Каталог
            </NavLink>
            <NavLink to="/deals" className={navLinkClasses}>
              Скидки недели
            </NavLink>
            <NavLink to={beaconsHref} className={navLinkClasses}>
              Мои маячки
            </NavLink>
          </nav>
        ) : (
          <HeaderSearch key={q} defaultValue={q} />
        )}
        <div className="ml-auto flex items-center gap-2 md:ml-0 md:gap-5">
          {variant === 'nav' && authenticated && (
            <>
              <Link to="/catalog" aria-label="Поиск" className={cn(iconButtonClasses(), 'md:hidden')}>
                <Search aria-hidden="true" size={20} />
              </Link>
              <Link to="/beacons" aria-label="Мои маячки" className={cn(iconButtonClasses(), 'md:hidden')}>
                <Bell aria-hidden="true" size={20} />
              </Link>
            </>
          )}
          {variant === 'search' && (
            <Link
              to={beaconsHref}
              aria-label="Мои маячки"
              className={cn(navLinkClasses({ isActive: false }), 'max-md:w-11 max-md:justify-center max-md:rounded-xl max-md:border max-md:border-line gap-2')}
            >
              <Bell aria-hidden="true" size={20} />
              <span className="max-md:hidden">Мои маячки</span>
            </Link>
          )}
          {account}
          <IconButton
            label="Основное меню"
            className="md:hidden"
            aria-haspopup="dialog"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen(true)}
          >
            <Menu aria-hidden="true" size={22} />
          </IconButton>
        </div>
        <MobileMenu open={menuOpen} onClose={() => setMenuOpen(false)} />
      </div>
    </header>
  )
}
