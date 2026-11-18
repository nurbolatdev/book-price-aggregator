import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { server } from '../../mocks/server'
import { renderAdmin } from '../../test/admin'
import { useNavigate } from 'react-router-dom'
import { getLocation, renderWithProviders } from '../../test/render'
import UsersPage from './UsersPage'

function JumpButton({ to }: { to: string }) {
  const navigate = useNavigate()
  return (
    <button type="button" onClick={() => navigate(to)}>
      jump
    </button>
  )
}

afterEach(() => {
  server.events.removeAllListeners()
})

function logUsers(): URL[] {
  const log: URL[] = []
  server.events.on('request:start', ({ request }) => {
    const url = new URL(request.url)
    if (url.pathname.endsWith('/admin/users')) log.push(url)
  })
  return log
}

const bodyRows = () => screen.getAllByRole('row').slice(1)
const last = (log: URL[]) => log[log.length - 1]

describe('UsersPage', () => {
  it('shows the summary, the first page and the verification badges', async () => {
    renderAdmin('/admin/users')
    expect(await screen.findByRole('heading', { level: 1, name: 'Пользователи' })).toBeInTheDocument()
    expect(
      await screen.findByText('Всего 128 · новых за неделю 14 · подтвердили почту 117'),
    ).toBeInTheDocument()
    await screen.findByText('nurbo@example.com')
    expect(bodyRows()).toHaveLength(20)
    expect(within(bodyRows()[0]!).getByText('подтверждён')).toBeInTheDocument()
    expect(within(bodyRows()[3]!).getByText('не подтверждён')).toBeInTheDocument()
    expect(screen.getByText('Показаны 1–20 из 45 · по 20 на странице')).toBeInTheDocument()
  })

  it('pages through the list and keeps the page in the URL', async () => {
    const log = logUsers()
    renderAdmin('/admin/users')
    await screen.findByText('nurbo@example.com')
    fireEvent.click(screen.getByRole('button', { name: 'Вперёд' }))
    await screen.findByText('Показаны 21–40 из 45 · по 20 на странице')
    expect(getLocation()).toContain('page=2')
    expect(last(log)?.searchParams.get('page')).toBe('1')
  })

  it('debounces the search and resets the page', async () => {
    const log = logUsers()
    renderAdmin('/admin/users?page=2')
    await screen.findByText('Показаны 21–40 из 45 · по 20 на странице')
    const before = log.length
    fireEvent.change(screen.getByRole('searchbox', { name: 'Поиск по email или имени' }), {
      target: { value: 'aigerim' },
    })
    await new Promise((r) => setTimeout(r, 150))
    expect(log).toHaveLength(before)
    await screen.findByText('aigerim.s@example.com')
    expect(bodyRows()).toHaveLength(1)
    expect(log.slice(before)).toHaveLength(1)
    expect(last(log)?.searchParams.get('q')).toBe('aigerim')
    expect(last(log)?.searchParams.get('page')).toBe('0')
    expect(getLocation()).toBe('/admin/users?q=aigerim')
  })

  it('syncs the search box when the URL changes under it', async () => {
    renderWithProviders(
      <>
        <UsersPage />
        <JumpButton to="/admin/users?q=aigerim" />
      </>,
      { route: '/admin/users?q=nurbo' },
    )
    const box = await screen.findByRole('searchbox', { name: 'Поиск по email или имени' })
    expect(box).toHaveValue('nurbo')
    fireEvent.click(screen.getByRole('button', { name: 'jump' }))
    await waitFor(() => expect(box).toHaveValue('aigerim'))
    expect(getLocation()).toBe('/admin/users?q=aigerim')
  })

  it('filters by verification and restores the state from the URL', async () => {
    const log = logUsers()
    renderAdmin('/admin/users')
    await screen.findByText('nurbo@example.com')
    fireEvent.click(screen.getByRole('button', { name: 'Не подтверждённые' }))
    await waitFor(() => expect(last(log)?.searchParams.get('verified')).toBe('false'))
    expect(getLocation()).toContain('verified=false')
    await waitFor(() => {
      for (const r of bodyRows()) expect(within(r).getByText('не подтверждён')).toBeInTheDocument()
    })
  })

  it('restores the chip and the page from the URL', async () => {
    const log = logUsers()
    renderAdmin('/admin/users?verified=true&page=2')
    await screen.findByText(/Показаны 21–/)
    expect(screen.getByRole('button', { name: 'Подтверждённые' })).toHaveAttribute('aria-pressed', 'true')
    expect(log[0]?.searchParams.get('verified')).toBe('true')
    expect(log[0]?.searchParams.get('page')).toBe('1')
  })

  it('says so when nobody matches', async () => {
    renderAdmin('/admin/users?q=zzzz')
    expect(await screen.findByText('Никого не нашли')).toBeInTheDocument()
  })
})
