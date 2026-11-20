import { act, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { OfflineBanner } from './OfflineBanner'

function setOnLine(value: boolean) {
  vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(value)
  act(() => {
    window.dispatchEvent(new Event(value ? 'online' : 'offline'))
  })
}

describe('OfflineBanner', () => {
  afterEach(() => vi.restoreAllMocks())

  it('shows nothing while online', () => {
    render(<OfflineBanner />)
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('shows the banner offline and hides it again online', () => {
    render(<OfflineBanner />)
    setOnLine(false)
    expect(screen.getByRole('status')).toHaveTextContent(
      'Нет подключения к интернету. Показываем сохранённые данные.',
    )
    setOnLine(true)
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})
