import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { logout } from '../../api/auth'
import { cn } from '../../lib/cn'
import { focusRing } from '../ui/focusRing'
import { useAuthStore } from '../../stores/auth'

const itemClasses = cn(
  'flex min-h-touch items-center rounded-lg px-3 text-[15px] text-ink hover:bg-canvas',
  focusRing,
)

export function UserMenu({ className }: { className?: string }) {
  const user = useAuthStore((s) => s.user)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const buttonRef = useRef<HTMLButtonElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)

  function items(): HTMLElement[] {
    return Array.from(menuRef.current?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? [])
  }

  useEffect(() => {
    if (open) items()[0]?.focus()
  }, [open])

  useEffect(() => {
    if (!open) return
    function onPointerDown(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    return () => document.removeEventListener('mousedown', onPointerDown)
  }, [open])

  if (!user) return null

  const initial = (user.name || user.email || '?').trim().charAt(0).toUpperCase() || '?'

  function onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Escape') {
      event.preventDefault()
      setOpen(false)
      buttonRef.current?.focus()
    } else if (event.key === 'Tab') {
      setOpen(false)
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      const list = items()
      if (list.length === 0) return
      const index = list.indexOf(document.activeElement as HTMLElement)
      const step = event.key === 'ArrowDown' ? 1 : -1
      list[(index + step + list.length) % list.length]?.focus()
    }
  }

  async function onLogout() {
    setOpen(false)
    await logout()
    queryClient.clear()
    navigate('/')
  }

  return (
    <div ref={rootRef} className={cn('relative', className)} onKeyDown={onKeyDown}>
      <button
        ref={buttonRef}
        type="button"
        aria-label="Меню пользователя"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
        className={cn(
          'inline-flex h-9 w-9 items-center md:h-11 md:w-11 justify-center rounded-full bg-ink text-sm md:text-base font-semibold text-white',
          focusRing,
        )}
      >
        {initial}
      </button>
      {open && (
        <div
          ref={menuRef}
          role="menu"
          aria-label="Меню пользователя"
          className="absolute right-0 top-full z-30 mt-2 w-60 rounded-[14px] border border-line bg-surface p-2 shadow-menu"
        >
          {user.email && <p className="truncate px-3 py-2 text-[13px] text-muted">{user.email}</p>}
          <Link role="menuitem" to="/beacons" className={itemClasses} onClick={() => setOpen(false)}>
            Мои маячки
          </Link>
          <Link role="menuitem" to="/profile" className={itemClasses} onClick={() => setOpen(false)}>
            Профиль и настройки
          </Link>
          {user.role === 'ADMIN' && (
            <Link role="menuitem" to="/admin" className={itemClasses} onClick={() => setOpen(false)}>
              Админ-панель
            </Link>
          )}
          <button
            type="button"
            role="menuitem"
            onClick={() => void onLogout()}
            className={cn(itemClasses, 'w-full text-left text-danger')}
          >
            Выйти
          </button>
        </div>
      )}
    </div>
  )
}
