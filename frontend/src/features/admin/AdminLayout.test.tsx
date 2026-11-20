import { screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { resetViewport, setViewport } from '../../test/viewport'
import { AdminLayout } from './AdminLayout'

function renderLayout(route: string) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <Routes>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<p>content</p>} />
          <Route path="parsers" element={<p>parsers</p>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('AdminLayout', () => {
  beforeEach(() => setViewport(1280))

  it('shows the menu with seven links and a way back to the site', () => {
    renderLayout('/admin')
    const nav = screen.getByRole('navigation', { name: 'Админ-меню' })
    const links = within(nav).getAllByRole('link')
    expect(links).toHaveLength(8)
    expect(within(nav).getByRole('link', { name: '← На сайт' })).toHaveAttribute('href', '/')
    expect(within(nav).getByRole('link', { name: 'Журнал событий' })).toHaveAttribute('href', '/admin/events')
  })

  it('marks the overview as current at /admin', () => {
    renderLayout('/admin')
    expect(screen.getByRole('link', { name: 'Обзор' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Парсеры' })).not.toHaveAttribute('aria-current')
  })

  it('marks only parsers as current at /admin/parsers', () => {
    renderLayout('/admin/parsers')
    expect(screen.getByRole('link', { name: 'Парсеры' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Обзор' })).not.toHaveAttribute('aria-current')
  })

  it('has no plaque on a wide screen and no site header or footer', () => {
    renderLayout('/admin')
    expect(screen.queryByText('Админ-панель рассчитана на экран от 1024 px')).toBeNull()
    expect(screen.queryByRole('banner')).toBeNull()
    expect(screen.queryByRole('contentinfo')).toBeNull()
  })

  it('warns on a narrow screen', () => {
    resetViewport()
    renderLayout('/admin')
    expect(screen.getByText('Админ-панель рассчитана на экран от 1024 px')).toBeInTheDocument()
  })
})
