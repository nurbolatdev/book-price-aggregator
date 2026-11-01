import { fireEvent, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../../app/App'
import { renderWithProviders } from '../../test/render'
import { resetViewport, setViewport } from '../../test/viewport'

describe('ScrollToTop', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    resetViewport()
  })

  it('scrolls to the top when a similar book is opened', async () => {
    setViewport(1280)
    const scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    renderWithProviders(<App />, { route: '/editions/41' })
    const card = await screen.findByRole('link', { name: /Собачье сердце/ })
    scrollTo.mockClear()
    fireEvent.click(card)
    await screen.findByRole('heading', { level: 1 })
    expect(scrollTo).toHaveBeenCalledWith(0, 0)
  })
})
