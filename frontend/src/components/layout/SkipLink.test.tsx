import { fireEvent, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../app/App'
import { useAuthStore } from '../../stores/auth'
import { renderAdmin } from '../../test/admin'
import { getLocation, renderWithProviders } from '../../test/render'

const FOCUSABLE = 'a[href], button, input, select, textarea, [tabindex]:not([tabindex="-1"])'

describe('SkipLink', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('is the first focusable element and moves focus to main without a hash', async () => {
    renderWithProviders(<App />, { route: '/' })
    await screen.findByRole('heading', { level: 1 })
    const link = screen.getByRole('link', { name: 'Перейти к содержимому' })
    expect(document.querySelector(FOCUSABLE)).toBe(link)
    fireEvent.click(link)
    expect(document.activeElement).toBe(document.getElementById('main'))
    expect(getLocation()).toBe('/')
  })

  it('works in the admin layout', async () => {
    renderAdmin('/admin')
    const link = await screen.findByRole('link', { name: 'Перейти к содержимому' })
    expect(document.querySelector(FOCUSABLE)).toBe(link)
    fireEvent.click(link)
    expect(document.activeElement).toBe(document.getElementById('main'))
    expect(getLocation()).toBe('/admin')
  })
})
