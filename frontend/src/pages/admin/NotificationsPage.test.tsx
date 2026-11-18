import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import { afterEach, describe, expect, it } from 'vitest'
import { server } from '../../mocks/server'
import { renderAdmin } from '../../test/admin'
import { getLocation } from '../../test/render'

afterEach(() => {
  server.events.removeAllListeners()
})

function logRequests(): { method: string; url: URL }[] {
  const log: { method: string; url: URL }[] = []
  server.events.on('request:start', ({ request }) => {
    const url = new URL(request.url)
    if (url.pathname.includes('/admin/notifications')) log.push({ method: request.method, url })
  })
  return log
}

const bodyRows = () => screen.getAllByRole('row').slice(1)
const rowAt = (i: number): HTMLElement => {
  const row = bodyRows()[i]
  if (!row) throw new Error(`no row ${i}`)
  return row
}

async function kpi(label: string): Promise<HTMLElement> {
  const card = (await screen.findByText(label)).parentElement
  if (!card) throw new Error('no card')
  return card
}

describe('NotificationsPage', () => {
  it('shows the KPI cards', async () => {
    renderAdmin('/admin/notifications')
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Маячки и письма' }),
    ).toBeInTheDocument()
    const beacons = await kpi('Активные маячки')
    await waitFor(() => expect(within(beacons).getByText('312')).toBeInTheDocument())
    expect(within(beacons).getByText('сработали за 24 ч: 9')).toBeInTheDocument()
    const emails = await kpi('Писем отправлено за 24 ч')
    expect(await within(emails).findByText('27')).toBeInTheDocument()
    expect(within(emails).getByText('ошибок: 1')).toBeInTheDocument()
    const waiting = await kpi('Ждут отправки')
    expect(await within(waiting).findByText('3')).toBeInTheDocument()
    expect(await within(waiting).findByText('из них в сводке: 2')).toBeInTheDocument()
  })

  it('lists the log and offers a retry only for failed rows', async () => {
    renderAdmin('/admin/notifications')
    await screen.findByText('Мастер и Маргарита')
    const rows = bodyRows()
    expect(rows).toHaveLength(20)
    expect(within(rowAt(0)).getByText('сразу')).toBeInTheDocument()
    expect(within(rowAt(0)).getByText('Отправлено')).toBeInTheDocument()
    expect(within(rowAt(1)).getByText('сводка')).toBeInTheDocument()
    expect(within(rowAt(1)).getByText('В очереди')).toBeInTheDocument()
    expect(within(rowAt(2)).getByText('Ошибка')).toBeInTheDocument()
    expect(within(rowAt(2)).getByText('SMTP: connection timed out')).toBeInTheDocument()
    expect(within(rowAt(3)).getByText('Сводка за день: 3 книги')).toBeInTheDocument()

    const failed = rows.filter((r) => within(r).queryByText('Ошибка'))
    expect(screen.getAllByRole('button', { name: 'Повторить' })).toHaveLength(failed.length)
    for (const row of rows) {
      const status = within(row).queryByText('Ошибка')
      expect(!!within(row).queryByRole('button', { name: 'Повторить' })).toBe(!!status)
    }
  })

  it('retries a failed email and refreshes the queue counter', async () => {
    const log = logRequests()
    renderAdmin('/admin/notifications')
    await screen.findByText('Атомные привычки')
    const row = rowAt(2)
    fireEvent.click(within(row).getByRole('button', { name: 'Повторить' }))
    await waitFor(() => expect(within(rowAt(2)).getByText('В очереди')).toBeInTheDocument())
    expect(log.filter((r) => r.method === 'POST')).toHaveLength(1)
    expect(log.find((r) => r.method === 'POST')?.url.pathname).toMatch(
      /\/admin\/notifications\/103\/retry$/,
    )
    expect(within(rowAt(2)).queryByRole('button', { name: 'Повторить' })).not.toBeInTheDocument()
    expect(within(rowAt(2)).getByText('—', { selector: 'td' })).toBeInTheDocument()
    const waiting = await kpi('Ждут отправки')
    expect(await within(waiting).findByText('4')).toBeInTheDocument()
  })

  it('shows a row message when the retry is refused', async () => {
    server.use(
      http.post('*/api/v1/admin/notifications/:id/retry', () =>
        HttpResponse.json({ code: 'CONFLICT', message: 'x' }, { status: 409 }),
      ),
    )
    renderAdmin('/admin/notifications')
    await screen.findByText('Атомные привычки')
    fireEvent.click(within(rowAt(2)).getByRole('button', { name: 'Повторить' }))
    expect(await within(rowAt(2)).findByRole('alert')).toHaveTextContent(
      'Повторить нельзя: письмо уже не в статусе «Ошибка» или почта не подтверждена',
    )
    expect(within(rowAt(2)).getByText('Ошибка')).toBeInTheDocument()
  })

  it('filters by status through the URL', async () => {
    const log = logRequests()
    renderAdmin('/admin/notifications')
    await screen.findByText('Атомные привычки')
    fireEvent.click(screen.getByRole('button', { name: 'Ошибка' }))
    await waitFor(() => expect(getLocation()).toBe('/admin/notifications?status=FAILED'))
    await waitFor(() => {
      const rows = bodyRows()
      expect(rows.length).toBeGreaterThan(0)
      for (const r of rows) expect(within(r).getByText('Ошибка')).toBeInTheDocument()
    })
    expect(
      log.some(
        (r) =>
          r.url.searchParams.get('status') === 'FAILED' && r.url.searchParams.get('size') === '20',
      ),
    ).toBe(true)
  })

  it('reloads the list on refresh', async () => {
    const log = logRequests()
    renderAdmin('/admin/notifications')
    await screen.findByText('Атомные привычки')
    const before = log.filter((r) => r.url.searchParams.get('size') === '20').length
    fireEvent.click(screen.getByRole('button', { name: 'Обновить' }))
    await waitFor(() =>
      expect(log.filter((r) => r.url.searchParams.get('size') === '20').length).toBe(before + 1),
    )
  })
})
