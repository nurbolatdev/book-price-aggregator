import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { facets, stores } from '../../mocks/fixtures'
import { FiltersForm } from './FiltersForm'
import { parseFilters, type SearchFilters } from './useSearchFilters'

const enabled = stores.filter((s) => s.enabled)

function base(extra: Partial<SearchFilters> = {}): SearchFilters {
  return { ...parseFilters(new URLSearchParams('q=abc'), 'search'), ...extra }
}

function setup(value: SearchFilters = base()) {
  const onChange = vi.fn()
  render(<FiltersForm layout="list" value={value} onChange={onChange} facets={facets} stores={enabled} />)
  return onChange
}

describe('FiltersForm', () => {
  it('renders counts from facets', () => {
    setup()
    expect(screen.getByRole('checkbox', { name: /Flip\.kz/ })).toHaveAccessibleName(/6/)
    expect(screen.getByRole('checkbox', { name: /Ozon/ })).toHaveAccessibleName(/8/)
    expect(screen.getByRole('checkbox', { name: /Твёрдый/ })).toHaveAccessibleName(/7/)
    expect(screen.getByRole('checkbox', { name: /Русский/ })).toHaveAccessibleName(/11/)
  })

  it('keeps a zero-count value visible but disabled', () => {
    setup()
    expect(screen.getByRole('checkbox', { name: /English/ })).toBeDisabled()
  })

  it('keeps a checked zero-count value enabled', () => {
    setup(base({ language: ['en'] }))
    expect(screen.getByRole('checkbox', { name: /English/ })).toBeEnabled()
  })

  it('disables the last checked store', () => {
    setup(base({ stores: ['FLIP'] }))
    expect(screen.getByRole('checkbox', { name: /Flip\.kz/ })).toBeDisabled()
    expect(screen.getByRole('checkbox', { name: /Ozon/ })).toBeEnabled()
  })

  it('unchecking one store lists the others', () => {
    const onChange = setup()
    fireEvent.click(screen.getByRole('checkbox', { name: /Ozon/ }))
    expect(onChange).toHaveBeenCalledWith({ stores: ['FLIP', 'MELOMAN'] })
  })

  it('applies the price only on blur or Enter', () => {
    const onChange = setup()
    const min = screen.getByLabelText('от')
    for (const v of ['1', '10', '100']) fireEvent.change(min, { target: { value: v } })
    expect(onChange).not.toHaveBeenCalled()
    fireEvent.blur(min)
    expect(onChange).toHaveBeenCalledTimes(1)
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ minPrice: 100 }))
    fireEvent.change(screen.getByLabelText('до'), { target: { value: '500' } })
    fireEvent.keyDown(screen.getByLabelText('до'), { key: 'Enter' })
    expect(onChange).toHaveBeenCalledTimes(2)
  })
})
