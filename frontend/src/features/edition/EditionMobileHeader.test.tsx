import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useToastStore } from '../../stores/toast'
import { EditionMobileHeader } from './EditionMobileHeader'

function Where() {
  const { pathname, search } = useLocation()
  return <p data-testid="where">{pathname + search}</p>
}

function renderHeader(entries: string[], index?: number) {
  return render(
    <MemoryRouter initialEntries={entries} initialIndex={index}>
      <Where />
      <Routes>
        <Route path="*" element={<EditionMobileHeader title="Мастер и Маргарита" shareable />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('EditionMobileHeader', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
    Object.defineProperty(navigator, 'share', { value: undefined, configurable: true })
    Object.defineProperty(navigator, 'clipboard', { value: undefined, configurable: true })
    useToastStore.setState({ toasts: [] })
  })

  it('goes to the home page when there is no history', () => {
    renderHeader(['/editions/41'])
    fireEvent.click(screen.getByRole('button', { name: 'Назад' }))
    expect(screen.getByTestId('where').textContent).toBe('/')
  })

  it('goes back in history', () => {
    renderHeader(['/search?q=x', '/editions/41'], 1)
    fireEvent.click(screen.getByRole('button', { name: 'Назад' }))
    expect(screen.getByTestId('where').textContent).toBe('/search?q=x')
  })

  it('uses the native share sheet when present', async () => {
    const share = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'share', { value: share, configurable: true })
    renderHeader(['/editions/41'])
    fireEvent.click(screen.getByRole('button', { name: 'Поделиться' }))
    await waitFor(() => expect(share).toHaveBeenCalledTimes(1))
    expect(share).toHaveBeenCalledWith({ title: 'Мастер и Маргарита', url: window.location.href })
  })

  it('copies the link when sharing is not available', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
    renderHeader(['/editions/41'])
    fireEvent.click(screen.getByRole('button', { name: 'Поделиться' }))
    await waitFor(() => expect(writeText).toHaveBeenCalledWith(window.location.href))
    await waitFor(() =>
      expect(useToastStore.getState().toasts.map((t) => t.message)).toContain('Ссылка скопирована'),
    )
  })

  it('reports a failed copy', async () => {
    const writeText = vi.fn().mockRejectedValue(new Error('denied'))
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
    renderHeader(['/editions/41'])
    fireEvent.click(screen.getByRole('button', { name: 'Поделиться' }))
    await waitFor(() =>
      expect(useToastStore.getState().toasts.map((t) => t.message)).toContain('Не удалось скопировать ссылку'),
    )
  })
})
