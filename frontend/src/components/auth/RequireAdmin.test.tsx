import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import type { Role } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { RequireAdmin } from './RequireAdmin'

function Where() {
  const loc = useLocation()
  return <div data-testid="where">{loc.pathname + loc.search}</div>
}

function setup() {
  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <Routes>
        <Route element={<RequireAdmin />}>
          <Route path="admin" element={<p>panel</p>} />
        </Route>
        <Route path="login" element={<Where />} />
        <Route path="/" element={<p>home</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

function signIn(role: Role) {
  useAuthStore.getState().setSession({
    accessToken: 'a',
    refreshToken: 'r',
    user: { id: 1, name: 'A', role },
  })
}

describe('RequireAdmin', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('redirects USER to home', () => {
    signIn('USER')
    setup()
    expect(screen.getByText('home')).toBeInTheDocument()
  })

  it('renders content for ADMIN', () => {
    signIn('ADMIN')
    setup()
    expect(screen.getByText('panel')).toBeInTheDocument()
  })

  it('redirects anonymous to login with next', () => {
    setup()
    expect(screen.getByTestId('where').textContent).toBe('/login?next=%2Fadmin')
  })
})
