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
})
