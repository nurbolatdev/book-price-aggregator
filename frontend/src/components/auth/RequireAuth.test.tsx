import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../../stores/auth'
import { RequireAuth } from './RequireAuth'

function Where() {
  const loc = useLocation()
  return <div data-testid="where">{loc.pathname + loc.search}</div>
}

function setup() {
  return render(
    <MemoryRouter initialEntries={['/beacons?tab=paused']}>
      <Routes>
        <Route element={<RequireAuth />}>
          <Route path="beacons" element={<p>secret</p>} />
        </Route>
        <Route path="login" element={<Where />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('RequireAuth', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('redirects anonymous to login with next', () => {
    setup()
    expect(screen.getByTestId('where').textContent).toBe('/login?next=%2Fbeacons%3Ftab%3Dpaused')
  })

  it('renders content when authenticated', () => {
    useAuthStore.getState().setSession({
      accessToken: 'a',
      refreshToken: 'r',
      user: { id: 1, name: 'A', role: 'USER' },
    })
    setup()
    expect(screen.getByText('secret')).toBeInTheDocument()
  })

  it('shows skeleton while restoring, without redirect', () => {
    useAuthStore.setState({ status: 'restoring' })
    setup()
    expect(screen.getByRole('status', { name: 'Загрузка' })).toBeInTheDocument()
    expect(screen.queryByTestId('where')).toBeNull()
    expect(screen.queryByText('secret')).toBeNull()
  })
})
