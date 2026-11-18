import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { mockAdmin } from '../../../mocks/adminFixtures'
import { ParsersTable } from './ParsersTable'
import type { ParserActions } from './useParserActions'

function actions(overrides: Partial<ParserActions> = {}): ParserActions {
  return {
    run: vi.fn(),
    runAll: vi.fn(),
    rowError: {},
    running: new Set(),
    runningAll: false,
    ...overrides,
  }
}

const rowOf = (name: string) => screen.getByRole('cell', { name }).closest('tr') as HTMLElement

describe('ParsersTable compact', () => {
  it('renders every store with a status badge', () => {
    render(<ParsersTable parsers={mockAdmin.parsers} variant="compact" actions={actions()} />)
    expect(screen.getAllByRole('row')).toHaveLength(6)
    expect(within(rowOf('Flip.kz')).getByText('OK')).toBeInTheDocument()
    expect(within(rowOf('Meloman')).getByText('Частичные ошибки')).toBeInTheDocument()
    expect(within(rowOf('Ozon')).getByText('Не отвечает')).toBeInTheDocument()
    expect(within(rowOf('Kaspi.kz')).getByText('Выключен')).toBeInTheDocument()
  })

  it('disables the restart button of a switched off store', () => {
    render(<ParsersTable parsers={mockAdmin.parsers} variant="compact" actions={actions()} />)
    const button = within(rowOf('Kaspi.kz')).getByRole('button', {
      name: 'Перезапустить',
    })
    expect(button).toBeDisabled()
    expect(button).toHaveAttribute('title', 'Сначала включите магазин')
    expect(within(rowOf('Flip.kz')).getByRole('button', { name: 'Перезапустить' })).toBeEnabled()
  })

  it('shows dashes for missing numbers', () => {
    render(<ParsersTable parsers={mockAdmin.parsers} variant="compact" actions={actions()} />)
    const cells = within(rowOf('Ozon')).getAllByRole('cell')
    expect(cells[4]).toHaveTextContent('—')
    expect(cells[5]).toHaveTextContent('—')
  })

  it('renders a row error as an alert in that row', () => {
    render(
      <ParsersTable
        parsers={mockAdmin.parsers}
        variant="compact"
        actions={actions({
          rowError: { FLIP: 'Очередь недоступна, попробуйте позже' },
        })}
      />,
    )
    expect(within(rowOf('Flip.kz')).getByRole('alert')).toHaveTextContent('Очередь недоступна')
    expect(within(rowOf('Meloman')).queryByRole('alert')).not.toBeInTheDocument()
  })
})
