import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { AdminHealth } from '../../../api/types'
import { health, mockAdmin } from '../../../mocks/adminFixtures'
import { HealthGrid } from './HealthGrid'

describe('HealthGrid', () => {
  it('renders six service cards in order with metrics', () => {
    render(<HealthGrid health={health} queues={mockAdmin.queues} />)
    const items = screen.getAllByRole('listitem')
    expect(items).toHaveLength(6)
    expect(
      items.map(
        (li) =>
          within(li).getByText(/^(Backend API|PostgreSQL|Redis|RabbitMQ|SMTP \(почта\)|Планировщик)$/)
            .textContent,
      ),
    ).toEqual(['Backend API', 'PostgreSQL', 'Redis', 'RabbitMQ', 'SMTP (почта)', 'Планировщик'])
    expect(screen.getAllByText('Работает')).toHaveLength(6)
    expect(screen.getByText('p95 ответа: 184 мс')).toBeInTheDocument()
    expect(screen.getByText('подключений: 3/20')).toBeInTheDocument()
    expect(screen.getByText('память: 12 МБ')).toBeInTheDocument()
    expect(screen.getByText('консьюмеров: 8')).toBeInTheDocument()
  })

  it('shows a failing component with its error text', () => {
    const down: AdminHealth = {
      ...health,
      components: {
        ...health.components,
        db: { status: 'DOWN', details: 'Connection refused' },
      },
    }
    render(<HealthGrid health={down} />)
    const card = screen.getByText('PostgreSQL').closest('li') as HTMLElement
    expect(within(card).getByText('Не отвечает')).toBeInTheDocument()
    expect(within(card).getByText('Connection refused')).toBeInTheDocument()
  })

  it('shows no data for a missing component', () => {
    const components = { ...health.components }
    delete components.scheduler
    render(<HealthGrid health={{ ...health, components }} />)
    const card = screen.getByText('Планировщик').closest('li') as HTMLElement
    expect(within(card).getByText('Нет данных')).toBeInTheDocument()
  })
})
