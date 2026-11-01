import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button, buttonClasses } from './Button'
import { Input } from './Input'

export function NotFound() {
  const navigate = useNavigate()
  const [q, setQ] = useState('')

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmed = q.trim()
    if (trimmed.length < 2) return
    navigate('/search?q=' + encodeURIComponent(trimmed))
  }

  return (
    <div className="flex flex-col items-center gap-4 text-center">
      <p aria-hidden="true" className="font-display text-[64px] font-bold leading-none text-accent">
        404
      </p>
      <h1 className="font-display text-2xl font-medium text-ink">Такой страницы нет</h1>
      <p className="max-w-md text-[15px] text-muted">
        Возможно, ссылка устарела или в адресе опечатка. Попробуйте найти книгу через поиск.
      </p>
      <form role="search" onSubmit={onSubmit} className="flex w-full max-w-md flex-col gap-2 md:flex-row">
        <Input
          type="search"
          aria-label="Поиск книги"
          placeholder="Название, автор или ISBN"
          minLength={2}
          maxLength={200}
          required
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <Button type="submit" variant="dark">
          Найти
        </Button>
      </form>
      <Link to="/" className={buttonClasses({ variant: 'ghost' })}>
        На главную
      </Link>
    </div>
  )
}
