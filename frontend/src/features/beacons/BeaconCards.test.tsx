import { fireEvent, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { describe, expect, it, vi } from 'vitest'
import type { BeaconResponse } from '../../api/types'
import { setViewport } from '../../test/viewport'
import { BeaconRow } from './BeaconRow'
import { TriggeredBeaconCard } from './TriggeredBeaconCard'

const now = new Date('2026-11-17T12:00:00Z')

const row: BeaconResponse = {
  id: 2,
  editionId: 7,
  title: 'Атомные привычки',
  author: 'Джеймс Клир',
  status: 'ACTIVE',
  conditionType: 'ANY_DROP',
  currentBestPrice: 5290,
  baselinePrice: 5990,
  statusChangedAt: '2026-11-01T08:00:00Z',
}

const triggered: BeaconResponse = {
  id: 1,
  editionId: 16,
  title: 'Мастер и Маргарита',
  status: 'TRIGGERED',
  conditionType: 'BELOW_PRICE',
  targetPrice: 3200,
  currentBestPrice: 3150,
  currentBestStore: 'FLIP',
  currentBestUrl: 'https://example.com/flip/16',
  lastTriggeredAt: '2026-11-17T09:12:00Z',
}

function renderUi(ui: ReactElement) {
  return render(<MemoryRouter>{ui}</MemoryRouter>)
}

function handlers() {
  return { onEdit: vi.fn(), onToggle: vi.fn(), onDelete: vi.fn() }
}

describe('BeaconRow', () => {
  it('shows the beacon and fires callbacks', () => {
    const h = handlers()
    renderUi(<BeaconRow beacon={row} now={now} {...h} />)
    expect(screen.getByRole('link', { name: 'Атомные привычки' })).toHaveAttribute('href', '/editions/7')
    expect(screen.getByText('Джеймс Клир')).toBeInTheDocument()
    expect(screen.getByText('Цель: любое снижение')).toBeInTheDocument()
    expect(screen.getByText('5 290 ₸')).toBeInTheDocument()
    expect(screen.getByText('было 5 990 ₸ при установке')).toBeInTheDocument()
    expect(screen.getByText('Ждёт')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Изменить условие' }))
    fireEvent.click(screen.getByRole('button', { name: 'Поставить на паузу' }))
    fireEvent.click(screen.getByRole('button', { name: 'Удалить маячок' }))
    expect(h.onEdit).toHaveBeenCalledWith(row)
    expect(h.onToggle).toHaveBeenCalledWith(row)
    expect(h.onDelete).toHaveBeenCalledWith(row)
  })

  it('offers resume for a paused beacon', () => {
    renderUi(<BeaconRow beacon={{ ...row, status: 'PAUSED' }} now={now} {...handlers()} />)
    expect(screen.getByRole('button', { name: 'Возобновить' })).toBeInTheDocument()
    expect(screen.getByText('На паузе')).toBeInTheDocument()
    expect(screen.getByText(/пауза с 1 ноября/)).toBeInTheDocument()
  })

  it('disables actions while the beacon is being saved', () => {
    renderUi(<BeaconRow beacon={{ ...row, id: -5 }} now={now} {...handlers()} />)
    for (const name of ['Изменить условие', 'Поставить на паузу', 'Удалить маячок']) {
      expect(screen.getByRole('button', { name })).toBeDisabled()
    }
  })

  it('exposes one pill and one price on the phone and on the desktop', () => {
    const { unmount } = renderUi(<BeaconRow beacon={row} now={now} {...handlers()} />)
    expect(screen.getAllByText('Ждёт')).toHaveLength(1)
    expect(screen.getAllByText('5 290 ₸')).toHaveLength(1)
    unmount()
    setViewport(1280)
    renderUi(<BeaconRow beacon={row} now={now} {...handlers()} />)
    expect(screen.getAllByText('Ждёт')).toHaveLength(1)
    expect(screen.getAllByText('5 290 ₸')).toHaveLength(1)
    expect(screen.getByText('Сейчас лучшая')).toBeInTheDocument()
  })
})

describe('TriggeredBeaconCard', () => {
  it('renders the cover tile', () => {
    renderUi(<TriggeredBeaconCard beacon={triggered} storeName={() => 'Flip.kz'} now={now} onFollow={vi.fn()} />)
    expect(screen.getByTestId('cover-plate')).toBeInTheDocument()
  })

  it('shows the trigger, goal, store and buy link', () => {
    const onFollow = vi.fn()
    renderUi(<TriggeredBeaconCard beacon={triggered} storeName={() => 'Flip.kz'} now={now} onFollow={onFollow} />)
    expect(screen.getByText(/Сработал сегодня в \d\d:\d\d/)).toBeInTheDocument()
    expect(screen.getByText(/Цель: ниже 3 200 ₸/)).toHaveTextContent('Цель: ниже 3 200 ₸ · сейчас 3 150 ₸ на Flip.kz')
    const buy = screen.getByRole('link', { name: /^Купить за 3\s150\s₸$/ })
    expect(buy).toHaveAttribute('href', 'https://example.com/flip/16')
    expect(buy).toHaveAttribute('target', '_blank')
    expect(buy).toHaveAttribute('rel', 'noopener noreferrer')
    fireEvent.click(screen.getByRole('button', { name: 'Следить дальше' }))
    expect(onFollow).toHaveBeenCalledWith(triggered)
  })

  it('has no buy link without a url', () => {
    renderUi(
      <TriggeredBeaconCard
        beacon={{ ...triggered, currentBestUrl: undefined }}
        storeName={() => 'Flip.kz'}
        now={now}
        onFollow={vi.fn()}
      />,
    )
    expect(screen.queryByRole('link', { name: /Купить/ })).toBeNull()
  })
})
