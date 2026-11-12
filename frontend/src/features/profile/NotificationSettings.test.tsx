import { fireEvent, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../../app/App'
import { MOCK_ACCESS_TOKEN, setMockSignedIn } from '../../mocks/authFixtures'
import { server } from '../../mocks/server'
import { useAuthStore } from '../../stores/auth'
import { useToastStore } from '../../stores/toast'
import { renderWithProviders } from '../../test/render'

const user = {
  id: 7,
  email: 'nurbo@example.com',
  name: 'Нурболат',
  role: 'USER' as const,
  emailVerified: true,
  notifyFrequency: 'INSTANT' as const,
  weeklyDigest: true,
}

function renderSettings() {
  setMockSignedIn(true)
  useAuthStore.getState().setSession({ accessToken: MOCK_ACCESS_TOKEN, user })
  return renderWithProviders(<App />, { route: '/profile' })
}

describe('NotificationSettings', () => {
  it('reflects the saved choices', async () => {
    renderSettings()
    expect(await screen.findByRole('radio', { name: 'Сразу, как маячок сработает' })).toBeChecked()
    expect(screen.getByRole('radio', { name: 'Одна сводка в день, в 9:00' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: /Еженедельная подборка/ })).toBeChecked()
  })

  it('switches at once and sends the frequency', async () => {
    const bodies: unknown[] = []
    let release: () => void = () => {}
    const gate = new Promise<void>((resolve) => {
      release = resolve
    })
    server.use(
      http.patch('*/api/v1/me', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>
        bodies.push(body)
        await gate
        return HttpResponse.json({ ...user, ...body })
      }),
    )
    renderSettings()
    const daily = await screen.findByRole('radio', { name: 'Одна сводка в день, в 9:00' })
    fireEvent.click(daily)
    await waitFor(() => expect(daily).toBeChecked())
    release()
    await waitFor(() => expect(bodies).toEqual([{ notifyFrequency: 'DAILY' }]))
  })

  it('rolls back and reports a failed save', async () => {
    server.use(
      http.patch('*/api/v1/me', () => HttpResponse.json({ code: 'INTERNAL_ERROR', message: 'x' }, { status: 500 })),
    )
    renderSettings()
    const daily = await screen.findByRole('radio', { name: 'Одна сводка в день, в 9:00' })
    fireEvent.click(daily)
    await waitFor(() =>
      expect(useToastStore.getState().toasts.map((t) => t.message)).toContain('Не удалось сохранить настройки'),
    )
    expect(screen.getByRole('radio', { name: 'Сразу, как маячок сработает' })).toBeChecked()
  })

  it('sends the weekly digest toggle', async () => {
    const bodies: unknown[] = []
    server.use(
      http.patch('*/api/v1/me', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>
        bodies.push(body)
        return HttpResponse.json({ ...user, ...body })
      }),
    )
    renderSettings()
    fireEvent.click(await screen.findByRole('checkbox', { name: /Еженедельная подборка/ }))
    await waitFor(() => expect(bodies).toEqual([{ weeklyDigest: false }]))
  })
})
