import { act, fireEvent, screen, waitFor } from '@testing-library/react'
import { delay, http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { server } from '../../mocks/server'
import { getLocation, renderWithProviders } from '../../test/render'
import { SearchBox } from './SearchBox'

function suggestResponse(q: string) {
  return [{ bookId: q.length, title: q, author: 'A', label: `Результат ${q}` }]
}

function type(value: string) {
  fireEvent.change(screen.getByRole('combobox'), { target: { value } })
}

describe('SearchBox', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('sends one suggest request per pause', async () => {
    const seen: string[] = []
    server.use(
      http.get('*/api/v1/search/suggest', ({ request }) => {
        const q = new URL(request.url).searchParams.get('q') ?? ''
        seen.push(q)
        return HttpResponse.json(suggestResponse(q))
      }),
    )
    renderWithProviders(<SearchBox variant="hero" />)
    for (const v of ['м', 'ма', 'мас', 'маст']) {
      type(v)
      act(() => {
        vi.advanceTimersByTime(50)
      })
    }
    act(() => {
      vi.advanceTimersByTime(300)
    })
    expect(await screen.findByRole('option', { name: 'Результат маст' })).toBeInTheDocument()
    expect(seen).toEqual(['маст'])
  })

  it('does not request for one character and shows a hint on submit', () => {
    let calls = 0
    server.use(
      http.get('*/api/v1/search/suggest', () => {
        calls += 1
        return HttpResponse.json([])
      }),
    )
    renderWithProviders(<SearchBox variant="hero" />)
    type('м')
    act(() => {
      vi.advanceTimersByTime(400)
    })
    fireEvent.submit(screen.getByRole('search'))
    expect(screen.getByText('Введите хотя бы 2 символа')).toBeInTheDocument()
    expect(screen.getByRole('combobox')).toHaveAccessibleDescription('Введите хотя бы 2 символа')
    expect(getLocation()).toBe('/')
    expect(calls).toBe(0)
  })

  it('moves the active option with arrows and opens the book on Enter', async () => {
    renderWithProviders(<SearchBox variant="header" />)
    type('ма')
    act(() => {
      vi.advanceTimersByTime(300)
    })
    await screen.findAllByRole('option')
    const input = screen.getByRole('combobox')
    const options = screen.getAllByRole('option')
    expect(options.length).toBeGreaterThan(1)
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    fireEvent.keyDown(input, { key: 'ArrowUp' })
    expect(options[0]).toHaveAttribute('aria-selected', 'true')
    expect(input).toHaveAttribute('aria-activedescendant', options[0]?.id)
    expect(options[1]).toHaveAttribute('aria-selected', 'false')
    fireEvent.keyDown(input, { key: 'Enter' })
    expect(getLocation()).toMatch(/^\/books\/\d+$/)
  })

  it('submits an encoded search without an active option', () => {
    renderWithProviders(<SearchBox variant="header" />)
    type('  Мастер и Маргарита ')
    fireEvent.submit(screen.getByRole('search'))
    expect(getLocation()).toBe('/search?q=' + encodeURIComponent('Мастер и Маргарита'))
  })

  it('closes the listbox on Escape', async () => {
    renderWithProviders(<SearchBox variant="header" />)
    type('ма')
    act(() => {
      vi.advanceTimersByTime(300)
    })
    await screen.findAllByRole('option')
    const input = screen.getByRole('combobox')
    expect(input).toHaveAttribute('aria-expanded', 'true')
    fireEvent.keyDown(input, { key: 'Escape' })
    expect(input).toHaveAttribute('aria-expanded', 'false')
  })

  it('does not let a stale answer overwrite a newer one', async () => {
    server.use(
      http.get('*/api/v1/search/suggest', async ({ request }) => {
        const q = new URL(request.url).searchParams.get('q') ?? ''
        await delay(q === 'ма' ? 600 : 50)
        return HttpResponse.json(suggestResponse(q))
      }),
    )
    renderWithProviders(<SearchBox variant="header" />)
    type('ма')
    act(() => {
      vi.advanceTimersByTime(300)
    })
    type('мас')
    act(() => {
      vi.advanceTimersByTime(300)
    })
    expect(await screen.findByRole('option', { name: 'Результат мас' })).toBeInTheDocument()
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 800))
    })
    await waitFor(() => expect(screen.getAllByRole('option')).toHaveLength(1))
    expect(screen.getByRole('option')).toHaveTextContent('Результат мас')
  })
})
