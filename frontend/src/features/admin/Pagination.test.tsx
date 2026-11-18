import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Pagination } from './Pagination'

describe('Pagination', () => {
  it('shows the default caption and pages forward and back', () => {
    const onChange = vi.fn()
    render(<Pagination page={1} size={20} totalElements={45} totalPages={3} onChange={onChange} />)
    expect(screen.getByText('Показаны 21–40 из 45')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Вперёд' }))
    expect(onChange).toHaveBeenLastCalledWith(2)
    fireEvent.click(screen.getByRole('button', { name: 'Назад' }))
    expect(onChange).toHaveBeenLastCalledWith(0)
  })

  it('disables back on the first page', () => {
    render(<Pagination page={0} size={20} totalElements={45} totalPages={3} onChange={() => {}} />)
    expect(screen.getByRole('button', { name: 'Назад' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Вперёд' })).toBeEnabled()
  })

  it('disables forward on the last page and clamps the caption', () => {
    render(<Pagination page={2} size={20} totalElements={45} totalPages={3} onChange={() => {}} />)
    expect(screen.getByRole('button', { name: 'Вперёд' })).toBeDisabled()
    expect(screen.getByText('Показаны 41–45 из 45')).toBeInTheDocument()
  })

  it('supports a custom caption', () => {
    render(
      <Pagination
        page={0}
        size={20}
        totalElements={23}
        totalPages={2}
        onChange={() => {}}
        caption={(from, to, total) => `${from}–${to} из ${total}`}
      />,
    )
    expect(screen.getByText('1–20 из 23')).toBeInTheDocument()
  })

  it('is hidden without items', () => {
    const { container } = render(<Pagination page={0} size={20} totalElements={0} totalPages={0} onChange={() => {}} />)
    expect(container).toBeEmptyDOMElement()
  })
})
