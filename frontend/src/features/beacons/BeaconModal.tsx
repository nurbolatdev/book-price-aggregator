import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useRef, useState, type MutableRefObject, type RefObject } from 'react'
import { useForm } from 'react-hook-form'
import { ApiError } from '../../api/errors'
import type { BeaconResponse, ConditionType } from '../../api/types'
import { Button } from '../../components/ui/Button'
import { Checkbox } from '../../components/ui/Checkbox'
import { Cover } from '../../components/ui/Cover'
import { controlAttrs } from '../../components/ui/Field'
import { Input } from '../../components/ui/Input'
import { Modal } from '../../components/ui/Modal'
import { applyFieldErrors } from '../../lib/applyFieldErrors'
import { formatPrice } from '../../lib/format'
import { toast } from '../../stores/toast'
import { useSingleSubmit } from '../auth/useSingleSubmit'
import { EmailBanner } from '../profile/EmailBanner'
import { useMe } from '../profile/useMe'
import type { BeaconBook } from './beaconBook'
import { beaconFormSchema, defaultValues, toConditionRequest, type BeaconFormValues } from './beaconSchema'
import { ConditionOption } from './ConditionOption'
import { useCreateBeacon } from './useCreateBeacon'
import { useUpdateBeacon } from './useUpdateBeacon'

interface BeaconModalProps {
  open: boolean
  onClose: () => void
  book: BeaconBook
  /** Existing beacon: edit mode. `null`: create mode. */
  beacon: BeaconResponse | null
}

const UNVERIFIED_TEXT = 'Почта не подтверждена — маячок сохранится, но письма придут после подтверждения'

export function BeaconModal({ open, onClose, book, beacon }: BeaconModalProps) {
  const initialFocusRef = useRef<HTMLInputElement | null>(null)
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={beacon ? 'Изменить маячок' : 'Маячок на книгу'}
      initialFocusRef={initialFocusRef as RefObject<HTMLElement>}
    >
      {open && <BeaconForm onClose={onClose} book={book} beacon={beacon} initialFocusRef={initialFocusRef} />}
    </Modal>
  )
}

interface BeaconFormProps {
  onClose: () => void
  book: BeaconBook
  beacon: BeaconResponse | null
  initialFocusRef: MutableRefObject<HTMLInputElement | null>
}

function BeaconForm({ onClose, book, beacon, initialFocusRef }: BeaconFormProps) {
  const me = useMe().data
  const create = useCreateBeacon()
  const update = useUpdateBeacon()
  const [initial] = useState(() => defaultValues(book, beacon))
  const schema = useMemo(() => beaconFormSchema(book.bestPrice), [book.bestPrice])
  const {
    register,
    handleSubmit,
    setError,
    watch,
    formState: { errors },
  } = useForm<BeaconFormValues>({ resolver: zodResolver(schema), defaultValues: initial })
  const selected = watch('conditionType')

  const submit = useSingleSubmit(
    handleSubmit(async (values) => {
      const condition = toConditionRequest(values)
      try {
        if (beacon) {
          await update.mutateAsync({ id: beacon.id ?? 0, editionId: book.editionId, patch: condition })
          toast.success('Маячок обновлён')
        } else {
          await create.mutateAsync({ request: { editionId: book.editionId, ...condition }, book })
        }
        onClose()
      } catch (error) {
        if (applyFieldErrors(error, setError, ['targetPrice', 'targetPercent'])) return
        if (error instanceof ApiError && error.code === 'CONFLICT') onClose()
      }
    }),
  )

  const pending = create.isPending || update.isPending
  const priceError = errors.targetPrice?.message
  const percentError = errors.targetPercent?.message
  const ids = { price: 'beacon-price-error', percent: 'beacon-percent-error' }

  function optionRef(type: ConditionType) {
    return (node: HTMLInputElement | null) => {
      if (type === initial.conditionType) initialFocusRef.current = node
    }
  }

  return (
    <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-col gap-[18px]">
      <div className="flex items-center gap-3.5 rounded-xl bg-canvas p-3">
        <Cover
          editionId={book.editionId}
          title={book.title}
          coverUrl={book.coverUrl}
          size="result"
          className="h-[68px] w-12 rounded-md"
        />
        <div className="flex min-w-0 flex-col gap-0.5">
          <strong>{book.title}</strong>
          {book.subtitle && <span className="text-sm text-muted">{book.subtitle}</span>}
          <span className="text-sm">
            {book.bestPrice != null ? (
              <>
                Сейчас от <strong>{formatPrice(book.bestPrice)}</strong>
              </>
            ) : (
              'Сейчас нет в наличии'
            )}
          </span>
        </div>
      </div>

      <fieldset>
        <legend className="mb-2.5 font-bold">Когда сообщить</legend>
        <div className="flex flex-col gap-2.5">
          <ConditionOption
            value="ANY_DROP"
            label="При любом снижении цены"
            selected={selected === 'ANY_DROP'}
            invalid={false}
            register={register('conditionType')}
            inputRef={optionRef('ANY_DROP')}
          />
          <ConditionOption
            value="BELOW_PRICE"
            label="Когда цена станет ниже"
            selected={selected === 'BELOW_PRICE'}
            invalid={Boolean(priceError)}
            register={register('conditionType')}
            inputRef={optionRef('BELOW_PRICE')}
          >
            {selected === 'BELOW_PRICE' && (
              <>
                <span className="ml-auto flex items-center gap-1.5">
                  <span className="w-[100px]">
                    <Input
                      inputMode="numeric"
                      aria-label="Желаемая цена, ₸"
                      {...controlAttrs({ id: 'beacon-price', describedBy: priceError ? ids.price : undefined, invalid: Boolean(priceError) })}
                      {...register('targetPrice')}
                    />
                  </span>
                  <span aria-hidden="true">₸</span>
                </span>
                {priceError && (
                  <p id={ids.price} className="basis-full text-[13px] text-danger">
                    {priceError}
                  </p>
                )}
              </>
            )}
          </ConditionOption>
          <ConditionOption
            value="DISCOUNT_PERCENT"
            label="При скидке от"
            selected={selected === 'DISCOUNT_PERCENT'}
            invalid={Boolean(percentError)}
            register={register('conditionType')}
            inputRef={optionRef('DISCOUNT_PERCENT')}
          >
            {selected === 'DISCOUNT_PERCENT' && (
              <>
                <span className="ml-auto flex items-center gap-1.5">
                  <span className="w-[70px]">
                    <Input
                      inputMode="numeric"
                      aria-label="Процент скидки"
                      {...controlAttrs({ id: 'beacon-percent', describedBy: percentError ? ids.percent : undefined, invalid: Boolean(percentError) })}
                      {...register('targetPercent')}
                    />
                  </span>
                  <span aria-hidden="true">%</span>
                </span>
                {percentError && (
                  <p id={ids.percent} className="basis-full text-[13px] text-danger">
                    {percentError}
                  </p>
                )}
              </>
            )}
          </ConditionOption>
        </div>
      </fieldset>

      <fieldset>
        <legend className="mb-2.5 font-bold">Куда отправить</legend>
        <div className="flex flex-col">
          <Checkbox label={`Email: ${me?.email ?? ''}`} checked disabled readOnly />
          <Checkbox label="Telegram — скоро" disabled />
        </div>
      </fieldset>

      <p className="text-[13px] leading-normal text-muted">
        Проверяем цены во всех магазинах каждые 6 часов. Отключить маячок можно в один клик из письма.
      </p>

      {me && me.emailVerified === false && <EmailBanner email={me.email ?? ''} text={UNVERIFIED_TEXT} />}

      <div className="flex gap-2.5 md:justify-end">
        <Button variant="secondary" size="lg" className="flex-1 md:flex-none" onClick={onClose}>
          Отмена
        </Button>
        <Button type="submit" size="lg" className="flex-1 md:flex-none" loading={pending}>
          {beacon ? 'Сохранить' : 'Поставить маячок'}
        </Button>
      </div>
    </form>
  )
}
