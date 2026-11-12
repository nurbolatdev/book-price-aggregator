import { StrictMode } from 'react'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import App from '../app/App'
import { queryKeys } from '../api/queryKeys'
import { MOCK_ACCESS_TOKEN } from '../mocks/authFixtures'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { renderWithProviders } from '../test/render'

const user = {
  id: 7,
  email: 'nurbo@example.com',
  name: 'Нурболат',
  role: 'USER' as const,
  emailVerified: false,
  notifyFrequency: 'INSTANT' as const,
  weeklyDigest: true,
}

function renderPage(query: string) {
  return renderWithProviders(
    <StrictMode>
      <App />
    </StrictMode>,
    { route: `/verify-email${query}` },
  )
}

function recordVerify() {
  const bodies: unknown[] = []
  server.use(
    http.post('*/api/v1/auth/verify-email', async ({ request }) => {
      bodies.push(await request.json())
      return new HttpResponse(null, { status: 204 })
    }),
  )
  return bodies
}

describe('VerifyEmailPage', () => {
  it('sends exactly one request under StrictMode and confirms', async () => {
    const bodies = recordVerify()
    renderPage('?token=valid')
    await screen.findByRole('heading', {
      level: 1,
      name: 'Почта подтверждена',
    })
    expect(bodies).toEqual([{ token: 'valid' }])
    expect(screen.getByRole('link', { name: 'К моим маячкам' })).toHaveAttribute('href', '/beacons')
    expect(screen.getByRole('link', { name: 'На главную' })).toHaveAttribute('href', '/')
  })

  it('names the signed-in address and marks the user verified everywhere', async () => {
    useAuthStore.getState().setSession({ accessToken: MOCK_ACCESS_TOKEN, user })
    const { queryClient } = renderPage('?token=valid')
    const strong = await screen.findByText('nurbo@example.com')
    expect(strong.tagName).toBe('STRONG')
    expect(useAuthStore.getState().user?.emailVerified).toBe(true)
    expect(queryClient.getQueryData<typeof user>(queryKeys.me)?.emailVerified).toBe(true)
  })

  it('shows the failure panel for an expired token', async () => {
    renderPage('?token=expired')
    await screen.findByRole('heading', {
      level: 1,
      name: 'Ссылка не сработала',
    })
  })

  it('lets a guest type the address and resend', async () => {
    const bodies: unknown[] = []
    server.use(
      http.post('*/api/v1/auth/resend-verification', async ({ request }) => {
        bodies.push(await request.json())
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderPage('?token=expired')
    await screen.findByRole('heading', {
      level: 1,
      name: 'Ссылка не сработала',
    })
    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'guest@example.com' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Отправить письмо ещё раз' }))
    await waitFor(() => expect(bodies).toEqual([{ email: 'guest@example.com' }]))
  })

  it('resends to the own address when signed in, without an email field', async () => {
    useAuthStore.getState().setSession({ accessToken: MOCK_ACCESS_TOKEN, user })
    const bodies: unknown[] = []
    server.use(
      http.post('*/api/v1/auth/resend-verification', async ({ request }) => {
        bodies.push(await request.json())
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderPage('?token=expired')
    await screen.findByRole('heading', {
      level: 1,
      name: 'Ссылка не сработала',
    })
    expect(screen.queryByLabelText('Email')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Отправить письмо ещё раз' }))
    await waitFor(() => expect(bodies).toEqual([{ email: 'nurbo@example.com' }]))
  })

  it('shows the failure panel without a request when the token is missing', async () => {
    const bodies = recordVerify()
    renderPage('')
    await screen.findByRole('heading', {
      level: 1,
      name: 'Ссылка не сработала',
    })
    expect(bodies).toEqual([])
  })

  it('offers a retry after a server error', async () => {
    let calls = 0
    server.use(
      http.post('*/api/v1/auth/verify-email', () => {
        calls += 1
        if (calls === 1) {
          return HttpResponse.json(
            { code: 'INTERNAL_ERROR', message: 'boom', fieldErrors: [] },
            { status: 500 },
          )
        }
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderPage('?token=valid')
    await screen.findByText('Не удалось подтвердить почту.')
    fireEvent.click(screen.getByRole('button', { name: 'Повторить' }))
    await screen.findByRole('heading', {
      level: 1,
      name: 'Почта подтверждена',
    })
    expect(calls).toBe(2)
  })
})
