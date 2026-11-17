import { fireEvent, screen, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MOCK_ACCESS_TOKEN, mockUser, patchMockUser, setMockSignedIn } from '../../mocks/authFixtures'
import { mockBeacons } from '../../mocks/beaconFixtures'
import { editionDetail } from '../../mocks/editionFixtures'
import { server } from '../../mocks/server'
import { useAuthStore } from '../../stores/auth'
import { useToastStore } from '../../stores/toast'
import { renderWithProviders } from '../../test/render'
import { setViewport } from '../../test/viewport'
import { BeaconModal } from './BeaconModal'
import { bookFromBeacon, bookFromEdition, type BeaconBook } from './beaconBook'
import type { BeaconResponse } from '../../api/types'

const book = (): BeaconBook => bookFromEdition(editionDetail(41)!)

function signIn(emailVerified: boolean) {
  setMockSignedIn(true)
  patchMockUser({ emailVerified })
  useAuthStore.getState().setSession({ accessToken: MOCK_ACCESS_TOKEN, user: { ...mockUser, emailVerified } })
}

function renderModal(options: { book?: BeaconBook; beacon?: BeaconResponse | null; onClose?: () => void } = {}) {
  const onClose = options.onClose ?? vi.fn()
  renderWithProviders(
    <BeaconModal open onClose={onClose} book={options.book ?? book()} beacon={options.beacon ?? null} />,
  )
  return onClose
}

function captureBodies(method: 'post' | 'patch', path: string) {
  const bodies: unknown[] = []
  server.use(
    http[method](path, async ({ request }) => {
      bodies.push(await request.json())
      return HttpResponse.json(
        { id: 99, editionId: 41, title: 'Мастер и Маргарита', status: 'ACTIVE', ...(bodies[0] as object) },
        { status: method === 'post' ? 201 : 200 },
      )
    }),
  )
  return bodies
}

const toasts = () => useToastStore.getState().toasts
const priceField = () => screen.getByLabelText('Желаемая цена, ₸')

beforeEach(() => {
  setViewport(1280)
  useToastStore.setState({ toasts: [] })
  signIn(true)
})

describe('BeaconModal create mode', () => {
  it('opens on BELOW_PRICE with a suggested price and focus on the checked radio', async () => {
    renderModal()
    expect(screen.getByRole('dialog', { name: 'Маячок на книгу' })).toBeInTheDocument()
    expect(screen.getByText('3 150 ₸')).toBeInTheDocument()
    const radio = screen.getByRole('radio', { name: 'Когда цена станет ниже' })
    expect(radio).toBeChecked()
    expect(radio).toHaveFocus()
    expect(priceField()).toHaveValue('2840')
    expect(screen.queryByLabelText('Процент скидки')).toBeNull()
  })

  it('shows the field of the selected option only', () => {
    renderModal()
    fireEvent.click(screen.getByRole('radio', { name: 'При скидке от' }))
    expect(screen.getByLabelText('Процент скидки')).toHaveValue('15')
    expect(screen.queryByLabelText('Желаемая цена, ₸')).toBeNull()
    fireEvent.click(screen.getByRole('radio', { name: 'При любом снижении цены' }))
    expect(screen.queryByLabelText('Процент скидки')).toBeNull()
    expect(screen.queryByLabelText('Желаемая цена, ₸')).toBeNull()
  })

  it.each([
    ['', 'Укажите цену'],
    ['0', 'Цена должна быть больше 0'],
    ['3150', 'Укажите цену ниже текущей — сейчас 3 150 ₸'],
  ])('rejects price %j without a request', async (value, message) => {
    const bodies = captureBodies('post', '*/api/v1/beacons')
    renderModal()
    fireEvent.change(priceField(), { target: { value } })
    fireEvent.click(screen.getByRole('button', { name: 'Поставить маячок' }))
    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(priceField()).toHaveAttribute('aria-invalid', 'true')
    expect(bodies).toEqual([])
  })

  it.each(['0', '91'])('rejects percent %s', async (value) => {
    renderModal()
    fireEvent.click(screen.getByRole('radio', { name: 'При скидке от' }))
    fireEvent.change(screen.getByLabelText('Процент скидки'), { target: { value } })
    fireEvent.click(screen.getByRole('button', { name: 'Поставить маячок' }))
    expect(await screen.findByText('Скидка — от 1 до 90%')).toBeInTheDocument()
  })

  it('posts the price condition, closes and toasts', async () => {
    const bodies = captureBodies('post', '*/api/v1/beacons')
    const onClose = renderModal()
    fireEvent.click(screen.getByRole('button', { name: 'Поставить маячок' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(bodies).toEqual([{ editionId: 41, conditionType: 'BELOW_PRICE', targetPrice: 2840 }])
    expect(toasts().map((t) => t.message)).toContain('Маячок поставлен')
  })

  it('posts a percent condition', async () => {
    const bodies = captureBodies('post', '*/api/v1/beacons')
    const onClose = renderModal()
    fireEvent.click(screen.getByRole('radio', { name: 'При скидке от' }))
    fireEvent.change(screen.getByLabelText('Процент скидки'), { target: { value: '20' } })
    fireEvent.click(screen.getByRole('button', { name: 'Поставить маячок' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(bodies).toEqual([{ editionId: 41, conditionType: 'DISCOUNT_PERCENT', targetPercent: 20 }])
  })

  it('posts ANY_DROP without target keys', async () => {
    const bodies = captureBodies('post', '*/api/v1/beacons')
    const onClose = renderModal()
    fireEvent.click(screen.getByRole('radio', { name: 'При любом снижении цены' }))
    fireEvent.click(screen.getByRole('button', { name: 'Поставить маячок' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(bodies).toEqual([{ editionId: 41, conditionType: 'ANY_DROP' }])
  })

  it('closes on a duplicate and offers to edit', async () => {
    const onClose = renderModal({ book: { ...book(), editionId: 16 } })
    fireEvent.click(screen.getByRole('button', { name: 'Поставить маячок' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    const toast = toasts().find((t) => t.message === 'На эту книгу маячок уже стоит')
    expect(toast?.action?.label).toBe('Изменить')
  })

  it('shows a server field error and stays open', async () => {
    server.use(
      http.post('*/api/v1/beacons', () =>
        HttpResponse.json(
          { code: 'VALIDATION_FAILED', message: 'x', fieldErrors: [{ field: 'targetPrice', message: 'цена слишком низкая' }] },
          { status: 422 },
        ),
      ),
    )
    const onClose = renderModal()
    fireEvent.click(screen.getByRole('button', { name: 'Поставить маячок' }))
    expect(await screen.findByText('цена слишком низкая')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('shows the delivery channels, the note and no warning for a verified user', () => {
    renderModal()
    const email = screen.getByRole('checkbox', { name: 'Email: nurbo@example.com' })
    expect(email).toBeChecked()
    expect(email).toBeDisabled()
    expect(screen.getByRole('checkbox', { name: 'Telegram — скоро' })).toBeDisabled()
    expect(screen.queryByText(/Почта не подтверждена/)).toBeNull()
  })

  it('warns an unverified user and offers to resend the letter', async () => {
    signIn(false)
    let resent = 0
    server.use(
      http.post('*/api/v1/auth/resend-verification', () => {
        resent += 1
        return new HttpResponse(null, { status: 204 })
      }),
    )
    renderModal()
    expect(await screen.findByText(/маячок сохранится, но письма придут после подтверждения/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Отправить письмо ещё раз' }))
    await waitFor(() => expect(resent).toBe(1))
  })

  it('cancels with the button and Esc without a request', () => {
    const bodies = captureBodies('post', '*/api/v1/beacons')
    const onClose = renderModal()
    fireEvent.click(screen.getByRole('button', { name: 'Отмена' }))
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(2)
    expect(bodies).toEqual([])
  })

  it('handles a book without a price', () => {
    renderModal({ book: { ...book(), bestPrice: null } })
    expect(screen.getByText('Сейчас нет в наличии')).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: 'При любом снижении цены' })).toBeChecked()
  })

  it('renders a bottom sheet on the phone', () => {
    setViewport(390)
    renderModal()
    const dialog = screen.getByRole('dialog', { name: 'Маячок на книгу' })
    expect(dialog.className).toContain('bottom-0')
    expect(screen.getByRole('radio', { name: 'Когда цена станет ниже' })).toBeInTheDocument()
  })
})

describe('BeaconModal edit mode', () => {
  const beacon = () => mockBeacons.find((b) => b.id === 3)!

  it('shows the saved condition and patches the new one', async () => {
    const bodies = captureBodies('patch', '*/api/v1/beacons/3')
    const onClose = renderModal({ book: bookFromBeacon(beacon()), beacon: beacon() })
    expect(screen.getByRole('dialog', { name: 'Изменить маячок' })).toBeInTheDocument()
    expect(priceField()).toHaveValue('8000')
    fireEvent.change(priceField(), { target: { value: '7500' } })
    fireEvent.click(screen.getByRole('button', { name: 'Сохранить' }))
    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(bodies).toEqual([{ conditionType: 'BELOW_PRICE', targetPrice: 7500 }])
    expect(toasts().map((t) => t.message)).toContain('Маячок обновлён')
  })
})
