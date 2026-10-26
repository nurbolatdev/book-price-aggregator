import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ErrorBoundary } from './ErrorBoundary'

let shouldThrow = true

function Child() {
  if (shouldThrow) throw new Error('boom')
  return <p>ok</p>
}

const fallback = (_e: Error, reset: () => void) => (
  <button onClick={reset}>Попробовать снова</button>
)

describe('ErrorBoundary', () => {
  beforeEach(() => {
    shouldThrow = true
    vi.spyOn(console, 'error').mockImplementation(() => undefined)
  })

  it('renders the fallback when a child throws', () => {
    render(
      <ErrorBoundary fallback={fallback}>
        <Child />
      </ErrorBoundary>,
    )
    expect(screen.getByRole('button', { name: 'Попробовать снова' })).toBeInTheDocument()
  })

  it('resets via the fallback button', () => {
    render(
      <ErrorBoundary fallback={fallback}>
        <Child />
      </ErrorBoundary>,
    )
    shouldThrow = false
    fireEvent.click(screen.getByRole('button', { name: 'Попробовать снова' }))
    expect(screen.getByText('ok')).toBeInTheDocument()
  })

  it('clears the error when resetKey changes', () => {
    function Host() {
      const { pathname } = useLocation()
      const navigate = useNavigate()
      return (
        <>
          <button onClick={() => navigate('/b')}>go</button>
          <ErrorBoundary resetKey={pathname} fallback={() => <p>fallback</p>}>
            <Child />
          </ErrorBoundary>
        </>
      )
    }
    render(
      <MemoryRouter initialEntries={['/a']}>
        <Host />
      </MemoryRouter>,
    )
    expect(screen.getByText('fallback')).toBeInTheDocument()
    shouldThrow = false
    fireEvent.click(screen.getByText('go'))
    expect(screen.getByText('ok')).toBeInTheDocument()
  })
})
