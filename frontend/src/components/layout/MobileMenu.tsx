import { Link, NavLink } from 'react-router-dom'
import { cn } from '../../lib/cn'
import { useAuthStore } from '../../stores/auth'
import { focusRing } from '../ui/focusRing'
import { Sheet } from '../ui/Sheet'

const itemClasses = cn(
  'flex h-12 items-center rounded-lg px-3 text-base font-medium text-ink hover:bg-canvas',
  focusRing,
)

interface MobileMenuProps {
  open: boolean
  onClose: () => void
}

export function MobileMenu({ open, onClose }: MobileMenuProps) {
  const status = useAuthStore((s) => s.status)
  const authenticated = status === 'authenticated'
  const beaconsHref = authenticated ? '/beacons' : '/login?next=%2Fbeacons'
  const linkClasses = ({ isActive }: { isActive: boolean }) =>
    cn(itemClasses, isActive && 'text-accent')

  return (
    <Sheet open={open} onClose={onClose} side="right" title="Меню">
      <nav aria-label="Мобильное меню" className="flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto p-2">
        <NavLink to="/catalog" className={linkClasses} onClick={onClose}>
          Каталог
        </NavLink>
        <NavLink to="/deals" className={linkClasses} onClick={onClose}>
          Скидки недели
        </NavLink>
        <NavLink to={beaconsHref} className={linkClasses} onClick={onClose}>
          Мои маячки
        </NavLink>
        {status === 'anonymous' && (
          <Link to="/login" className={itemClasses} onClick={onClose}>
            Войти
          </Link>
        )}
      </nav>
    </Sheet>
  )
}
