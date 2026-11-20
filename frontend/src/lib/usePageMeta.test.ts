import { renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { usePageMeta } from './usePageMeta'

describe('usePageMeta', () => {
  it('sets title and description and restores them on unmount', () => {
    document.title = 'Before'
    const tag = document.createElement('meta')
    tag.name = 'description'
    tag.content = 'Old text'
    document.head.appendChild(tag)

    const { unmount } = renderHook(() => usePageMeta({ title: 'New', description: 'New text' }))
    expect(document.title).toBe('New')
    expect(tag.content).toBe('New text')

    unmount()
    expect(document.title).toBe('Before')
    expect(tag.content).toBe('Old text')
    tag.remove()
  })

  it('creates missing og tags and restores them on unmount', () => {
    const { unmount } = renderHook(() =>
      usePageMeta({ title: 'T', description: 'D', image: 'https://img.test/c.jpg' }),
    )
    const og = (name: string) =>
      document.querySelector(`meta[property="${name}"]`)?.getAttribute('content')
    expect(og('og:title')).toBe('T')
    expect(og('og:description')).toBe('D')
    expect(og('og:image')).toBe('https://img.test/c.jpg')
    unmount()
    expect(og('og:title')).toBe('')
    expect(og('og:image')).toBe('')
    document.querySelectorAll('meta[property^="og:"]').forEach((t) => t.remove())
  })

  it('leaves og:image alone when no image is passed', () => {
    const tag = document.createElement('meta')
    tag.setAttribute('property', 'og:image')
    tag.content = '/icon-512.png'
    document.head.appendChild(tag)
    const { unmount } = renderHook(() => usePageMeta({ title: 'T', description: 'D' }))
    expect(tag.content).toBe('/icon-512.png')
    unmount()
    expect(tag.content).toBe('/icon-512.png')
    document.querySelectorAll('meta[property^="og:"]').forEach((t) => t.remove())
  })
})
