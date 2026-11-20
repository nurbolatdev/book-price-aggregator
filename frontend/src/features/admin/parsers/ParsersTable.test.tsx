import { fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { mockAdmin } from '../../../mocks/adminFixtures'
import { ParsersTable } from './ParsersTable'
import type { ParserActions } from './useParserActions'
import type { StoreToggle } from './useToggleStore'

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

describe('ParsersTable full', () => {
  const toggle = (): StoreToggle => ({
    toggle: vi.fn(),
    confirming: null,
    cancel: vi.fn(),
    confirm: vi.fn(),
    busy: new Set(),
    pending: false,
  })

  it('renders switches, breaker state and the run button', () => {
    const t = toggle()
    render(<ParsersTable parsers={mockAdmin.parsers} variant="full" actions={actions()} toggle={t} />)
    expect(screen.getAllByRole('columnheader').map((h) => h.textContent)).toEqual([
      'Магазин',
      'Вкл.',
      'Статус',
      'Запуск',
      'Успех 24 ч',
      'Предложений',
      'Ср. время',
      'Breaker',
      'Действия',
    ])
    expect(within(rowOf('Flip.kz')).getByRole('switch', { name: 'Магазин Flip.kz' })).toBeChecked()
    expect(within(rowOf('Kaspi.kz')).getByRole('switch', { name: 'Магазин Kaspi.kz' })).not.toBeChecked()
    expect(within(rowOf('Flip.kz')).getByText('закрыт')).toBeInTheDocument()
    expect(within(rowOf('Ozon')).getByText('открыт')).toBeInTheDocument()
    expect(within(rowOf('Flip.kz')).getByRole('button', { name: 'Запустить' })).toBeEnabled()
    expect(within(rowOf('Kaspi.kz')).getByRole('button', { name: 'Запустить' })).toBeDisabled()
  })

  it('reports a switch click', () => {
    const t = toggle()
    render(<ParsersTable parsers={mockAdmin.parsers} variant="full" actions={actions()} toggle={t} />)
    fireEvent.click(within(rowOf('Meloman')).getByRole('switch'))
    expect(t.toggle).toHaveBeenCalledWith(expect.objectContaining({ code: 'MELOMAN' }))
  })
})
