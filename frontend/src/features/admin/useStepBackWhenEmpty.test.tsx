import { renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { useStepBackWhenEmpty } from './useStepBackWhenEmpty'

function run(
  data: { content?: unknown[]; totalElements?: number } | undefined,
  isPlaceholder: boolean,
  page: number,
) {
  const changePage = vi.fn()
  renderHook(() => useStepBackWhenEmpty(data, isPlaceholder, page, 20, changePage))
  return changePage
}

describe('useStepBackWhenEmpty', () => {
  it('steps back to the last page, replacing the history entry', () => {
    const changePage = run({ content: [], totalElements: 45 }, false, 9)
    expect(changePage).toHaveBeenCalledWith(2, { replace: true })
  })

  it('does nothing for placeholder data', () => {
    expect(run({ content: [], totalElements: 45 }, true, 9)).not.toHaveBeenCalled()
  })

  it('does nothing on the first page', () => {
    expect(run({ content: [], totalElements: 45 }, false, 0)).not.toHaveBeenCalled()
  })

  it('does nothing when the list is empty', () => {
    expect(run({ content: [], totalElements: 0 }, false, 3)).not.toHaveBeenCalled()
  })
})
