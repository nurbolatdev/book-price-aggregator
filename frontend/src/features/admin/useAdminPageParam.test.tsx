import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { parsePageParam, useAdminPageParam } from './useAdminPageParam'

function setup(route: string) {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
  )
  return renderHook(
    () => {
      const [page, setPage] = useAdminPageParam()
      return { page, setPage, search: useLocation().search }
    },
    { wrapper },
  )
}

describe('useAdminPageParam', () => {
  it('treats missing, invalid and zero values as the first page', () => {
    expect(parsePageParam(null)).toBe(0)
    expect(parsePageParam('abc')).toBe(0)
    expect(parsePageParam('0')).toBe(0)
    expect(parsePageParam('-2')).toBe(0)
    expect(parsePageParam('1.5')).toBe(0)
  })

  it('converts the 1-based URL page to a 0-based number', () => {
    expect(setup('/x?page=3').result.current.page).toBe(2)
  })

  it('writes the page and removes the param for the first page', () => {
    const { result } = setup('/x?q=a&page=3')
    act(() => result.current.setPage(1))
    expect(result.current.search).toBe('?q=a&page=2')
    act(() => result.current.setPage(0))
    expect(result.current.search).toBe('?q=a')
  })
})
