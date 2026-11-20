import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { statsFor } from '../../../mocks/adminFixtures'
import { StatsKpis } from './StatsKpis'

const norm = (s: string | null | undefined) => (s ?? '').replace(/[\u00a0\u202f]/g, ' ')

describe('StatsKpis', () => {
  it('shows the 24 hour numbers and captions', () => {
    const { container } = render(<StatsKpis stats={statsFor('24h')} period="24h" />)
    const text = norm(container.textContent)
    expect(screen.getByText('Пользователи')).toBeInTheDocument()
    expect(text).toContain('128')
    expect(text).toContain('+3 за 24 ч')
    expect(screen.getByText('Активны за 24 ч')).toBeInTheDocument()
    expect(text).toContain('41')
    expect(text).toContain('активны за период')
    expect(text).toContain('ошибок доставки: 1')
    expect(text).toContain('попадание в кэш 72%')
    expect(text).toContain('1 842 / 4 105')
    expect(text).toContain('312')
    expect(text).toContain('9 сработали за 24 ч')
  })

  it('uses the week label for 7d', () => {
    render(<StatsKpis stats={statsFor('7d')} period="7d" />)
    expect(screen.getByText('Активны за 7 дней')).toBeInTheDocument()
    expect(screen.getByText('Поисков за 7 дней')).toBeInTheDocument()
    expect(screen.getByText('+14 за 7 дней')).toBeInTheDocument()
  })
})
