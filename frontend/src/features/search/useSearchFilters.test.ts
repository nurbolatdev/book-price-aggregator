import { act, renderHook } from '@testing-library/react'
import { createElement, type ReactNode } from 'react'
import { MemoryRouter, useLocation, useNavigate } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import {
  countActiveFilters,
  parseFilters,
  toApiParams,
  toFacetParams,
  useSearchFilters,
  type SearchMode,
} from './useSearchFilters'

function parse(search: string, mode: SearchMode = 'search') {
  return parseFilters(new URLSearchParams(search), mode)
}

function setup(route: string, mode: SearchMode = 'search') {
  const wrapper = ({ children }: { children: ReactNode }) =>
    createElement(MemoryRouter, { initialEntries: [route] }, children)
  return renderHook(
    () => ({
      api: useSearchFilters(mode),
      location: useLocation(),
      navigate: useNavigate(),
    }),
    { wrapper },
  )
}

describe('parseFilters', () => {
  it('parses every supported parameter', () => {
    const f = parse(
      'q=abc&stores=OZON,FLIP&binding=PAPERBACK,HARDCOVER&language=kk,ru&inStock=false&minPrice=1000&sort=PRICE_ASC',
    )
    expect(f).toEqual({
      q: 'abc',
      stores: ['FLIP', 'OZON'],
      binding: ['HARDCOVER', 'PAPERBACK'],
      language: ['ru', 'kk'],
      inStock: false,
      minPrice: 1000,
      maxPrice: undefined,
      sort: 'PRICE_ASC',
    })
  })

  it('drops invalid values', () => {
    const f = parse('q=abc&binding=FOO&stores=NOPE&minPrice=-5&maxPrice=abc&language=de&sort=BAD')
    expect(f.binding).toEqual([])
    expect(f.stores).toEqual([])
    expect(f.minPrice).toBeUndefined()
    expect(f.maxPrice).toBeUndefined()
    expect(f.language).toEqual([])
    expect(f.sort).toBe('RELEVANCE')
  })

  it('uses defaults and rejects RELEVANCE in catalog', () => {
    expect(parse('', 'catalog').sort).toBe('STORES_DESC')
    expect(parse('sort=RELEVANCE', 'catalog').sort).toBe('STORES_DESC')
    expect(parse('', 'search').inStock).toBe(true)
  })
})

describe('api params helpers', () => {
  it('builds api params with sort and facet params without it', () => {
    const f = parse('q=abc&stores=FLIP&inStock=false&sort=PRICE_ASC&maxPrice=5000')
    expect(toApiParams(f, 'search')).toEqual({
      q: 'abc',
      stores: ['FLIP'],
      inStock: false,
      maxPrice: 5000,
      sort: 'PRICE_ASC',
    })
    expect(toFacetParams(f, 'search')).not.toHaveProperty('sort')
  })

  it('omits q in catalog mode', () => {
    const f = parse('q=abc', 'catalog')
    expect(toFacetParams(f, 'catalog')).not.toHaveProperty('q')
    expect(toApiParams(f, 'catalog')).toEqual({ inStock: true, sort: 'STORES_DESC' })
  })

  it('counts non-default groups', () => {
    expect(countActiveFilters(parse('q=abc'))).toBe(0)
    expect(
      countActiveFilters(parse('q=abc&stores=FLIP&minPrice=1&maxPrice=9&inStock=false&binding=HARDCOVER&language=ru')),
    ).toBe(5)
  })
})

describe('useSearchFilters', () => {
  it('omits defaults and deletes page when writing', () => {
    const { result } = setup('/search?q=abc&page=3&stores=FLIP&sort=PRICE_ASC')
    act(() => result.current.api.setFilters({ inStock: true, stores: [], sort: 'RELEVANCE' }))
    expect(result.current.location.search).toBe('?q=abc')
  })

  it('writes csv values in canonical order', () => {
    const { result } = setup('/search?q=abc')
    act(() => result.current.api.setFilters({ stores: ['OZON', 'FLIP'], language: ['kk', 'ru'] }))
    const params = new URLSearchParams(result.current.location.search)
    expect(params.get('stores')).toBe('FLIP,OZON')
    expect(params.get('language')).toBe('ru,kk')
  })

  it('removes a price when set to undefined', () => {
    const { result } = setup('/search?q=abc&minPrice=100')
    act(() => result.current.api.setFilters({ minPrice: undefined }))
    expect(result.current.location.search).toBe('?q=abc')
  })

  it('pushes a history entry', () => {
    const { result } = setup('/search?q=abc&stores=FLIP')
    act(() => result.current.api.setFilters({ stores: ['FLIP', 'OZON'] }))
    expect(result.current.location.search).toContain('stores=FLIP%2COZON')
    act(() => result.current.navigate(-1))
    expect(result.current.location.search).toBe('?q=abc&stores=FLIP')
  })

  it('resets everything except q', () => {
    const { result } = setup('/search?q=abc&stores=FLIP&inStock=false&sort=PRICE_ASC')
    act(() => result.current.api.resetFilters())
    expect(result.current.location.search).toBe('?q=abc')
  })
})
