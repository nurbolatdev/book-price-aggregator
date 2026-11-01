import { useId, useState, type ChangeEvent, type FormEvent, type KeyboardEvent } from 'react'
import { Search } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { cn } from '../../lib/cn'
import { buttonClasses } from '../../components/ui/Button'
import { focusRing } from '../../components/ui/focusRing'
import { MIN_QUERY_LENGTH, useSuggestions } from './useSuggestions'

interface SearchBoxProps {
  variant: 'hero' | 'header'
  defaultValue?: string
}

export function SearchBox({ variant, defaultValue = '' }: SearchBoxProps) {
  const navigate = useNavigate()
  const uid = useId()
  const listId = `${uid}-list`
  const hintId = `${uid}-hint`
  const [value, setValue] = useState(defaultValue)
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(-1)
  const [hint, setHint] = useState(false)
  const [typed, setTyped] = useState(false)
  const suggestions = useSuggestions(value, typed)
  const expanded = open && suggestions.length > 0
  const activeIndex = expanded && active < suggestions.length ? active : -1
  const hero = variant === 'hero'

  function onChange(event: ChangeEvent<HTMLInputElement>) {
    setValue(event.target.value)
    setTyped(true)
    setOpen(true)
    setActive(-1)
    setHint(false)
  }

  function submit() {
    const trimmed = value.trim()
    if (trimmed.length < MIN_QUERY_LENGTH) {
      setHint(true)
      return
    }
    setOpen(false)
    navigate('/search?q=' + encodeURIComponent(trimmed))
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    submit()
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      if (suggestions.length === 0) return
      event.preventDefault()
      setOpen(true)
      const count = suggestions.length
      const delta = event.key === 'ArrowDown' ? 1 : -1
      setActive(activeIndex === -1 ? (delta === 1 ? 0 : count - 1) : (activeIndex + delta + count) % count)
    } else if (event.key === 'Enter' && activeIndex >= 0) {
      event.preventDefault()
      const picked = suggestions[activeIndex]
      if (!picked) return
      setOpen(false)
      navigate('/books/' + picked.bookId)
    } else if (event.key === 'Escape') {
      setOpen(false)
      setActive(-1)
    }
  }

  return (
    <form
      role="search"
      onSubmit={onSubmit}
      className={cn(
        'relative',
        hero
          ? 'flex w-full max-w-[860px] flex-col gap-3 md:flex-row md:gap-3'
          : 'flex-[1_1_360px] max-md:order-last max-md:basis-full',
      )}
    >
      <div className="relative flex-1">
        <div
          className={cn(
            'flex items-center gap-2.5',
            hero
              ? 'h-[52px] rounded-xl border-2 border-ink bg-surface px-3.5 md:h-[60px] md:rounded-[14px] md:px-[18px]'
              : 'relative',
          )}
        >
          <Search
            aria-hidden="true"
            size={hero ? 22 : 20}
            className={cn(
              'shrink-0 text-muted',
              !hero && 'pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2',
            )}
          />
          <input
            type="search"
            role="combobox"
            aria-label="Поиск книги"
            aria-autocomplete="list"
            aria-expanded={expanded}
            aria-controls={listId}
            aria-activedescendant={activeIndex >= 0 ? `${uid}-opt-${activeIndex}` : undefined}
            aria-describedby={hint ? hintId : undefined}
            autoComplete="off"
            maxLength={200}
            placeholder="Название, автор или ISBN"
            value={value}
            onChange={onChange}
            onKeyDown={onKeyDown}
            onFocus={() => setOpen(true)}
            onBlur={() => setOpen(false)}
            className={cn(
              'w-full text-ink placeholder:text-muted',
              hero
                ? 'h-full bg-transparent text-base focus-visible:outline-none md:text-lg'
                : cn('h-[46px] rounded-xl border border-line bg-canvas pl-11 pr-3.5 text-[15px]', focusRing),
            )}
          />
        </div>
        <ul
          id={listId}
          role="listbox"
          aria-label="Подсказки"
          hidden={!expanded}
          className="absolute left-0 right-0 top-full z-20 mt-1 overflow-hidden rounded-xl border border-line bg-surface p-1 shadow-menu"
        >
          {expanded &&
            suggestions.map((s, index) => (
              <li
                key={`${s.bookId}-${index}`}
                id={`${uid}-opt-${index}`}
                role="option"
                aria-selected={index === activeIndex}
                onMouseDown={(event) => {
                  event.preventDefault()
                  setOpen(false)
                  navigate('/books/' + s.bookId)
                }}
                className={cn(
                  'cursor-pointer rounded-lg px-3 py-2.5 text-[15px] text-ink',
                  index === activeIndex && 'bg-canvas',
                )}
              >
                {s.label}
              </li>
            ))}
        </ul>
        <p id={hintId} aria-live="polite" className={cn('mt-1 text-[13px] text-danger', !hint && 'sr-only')}>
          {hint ? 'Введите хотя бы 2 символа' : ''}
        </p>
      </div>
      {hero && (
        <button
          type="submit"
          className={cn(
            buttonClasses({ variant: 'primary', size: 'xl' }),
            'w-full md:h-[60px] md:w-auto md:rounded-[14px] md:px-8 md:text-lg',
          )}
        >
          Найти лучшую цену
        </button>
      )}
    </form>
  )
}
