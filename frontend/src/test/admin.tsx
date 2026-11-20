import type { RenderResult } from '@testing-library/react'
import type { QueryClient } from '@tanstack/react-query'
import App from '../app/App'
import { Toaster } from '../components/ui/Toaster'
import { MOCK_ACCESS_TOKEN, setMockSignedIn } from '../mocks/authFixtures'
import { useAuthStore } from '../stores/auth'
import { renderWithProviders } from './render'
import { setViewport } from './viewport'

export function signInAdmin(): void {
  setMockSignedIn(true)
  useAuthStore.getState().setSession({
    accessToken: MOCK_ACCESS_TOKEN,
    user: {
      id: 1,
      email: 'admin@example.com',
      name: 'Админ',
      role: 'ADMIN',
      emailVerified: true,
      notifyFrequency: 'INSTANT',
      weeklyDigest: true,
    },
  })
}

export function renderAdmin(route = '/admin'): RenderResult & { queryClient: QueryClient } {
  setViewport(1280)
  signInAdmin()
  return renderWithProviders(
    <>
      <App />
      <Toaster />
    </>,
    { route },
  )
}
