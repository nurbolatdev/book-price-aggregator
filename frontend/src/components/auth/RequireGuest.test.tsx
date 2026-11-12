import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from '../../stores/auth'
import { RequireGuest } from './RequireGuest'

function Where() {
  const loc = useLocation()
  return <div data-testid="where">{loc.pathname + loc.search}</div>
}

function setup(entry: string) {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route element={<RequireGuest />}>
          <Route path="login" element={<p>login page</p>} />
        </Route>
        <Route path="*" element={<Where />} />
      </Routes>
    </MemoryRouter>,
  )
}

const session = {
  accessToken: 'a',
  user: { id: 1, name: 'A', role: 'USER' as const },
}

describe('RequireGuest', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('sends a signed-in user to next', () => {
    useAuthStore.getState().setSession(session)
    setup('/login?next=%2Fbeacons')
    expect(screen.getByTestId('where').textContent).toBe('/beacons')
  })

  it('ignores an external next', () => {
    useAuthStore.getState().setSession(session)
    setup('/login?next=https%3A%2F%2Fevil.example')
    expect(screen.getByTestId('where').textContent).toBe('/')
  })

  it('shows the skeleton while restoring', () => {
    useAuthStore.setState({ status: 'restoring' })
    setup('/login')
    expect(screen.getByRole('status', { name: 'Загрузка' })).toBeInTheDocument()
  })

  it('renders the page for a guest', () => {
    setup('/login')
    expect(screen.getByText('login page')).toBeInTheDocument()
  })
})
