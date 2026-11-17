import { z } from 'zod'
import type { BeaconResponse, ConditionType } from '../../api/types'
import { formatPrice } from '../../lib/format'
import type { BeaconBook } from './beaconBook'

export interface BeaconFormValues {
  conditionType: ConditionType
  targetPrice: string
  targetPercent: string
}

const WHOLE_NUMBER = /^\d+$/

export function beaconFormSchema(bestPrice: number | null) {
  return z
    .object({
      conditionType: z.enum(['ANY_DROP', 'BELOW_PRICE', 'DISCOUNT_PERCENT']),
      targetPrice: z.string(),
      targetPercent: z.string(),
    })
    .superRefine((values, ctx) => {
      if (values.conditionType === 'BELOW_PRICE') {
        const raw = values.targetPrice.trim()
        const fail = (message: string) => ctx.addIssue({ code: 'custom', path: ['targetPrice'], message })
        if (raw === '') return fail('Укажите цену')
        if (!WHOLE_NUMBER.test(raw)) return fail('Цена — целое число в тенге')
        const price = Number(raw)
        if (price === 0) return fail('Цена должна быть больше 0')
        if (bestPrice != null && price >= bestPrice) {
          return fail(`Укажите цену ниже текущей — сейчас ${formatPrice(bestPrice)}`)
        }
      }
      if (values.conditionType === 'DISCOUNT_PERCENT') {
        const raw = values.targetPercent.trim()
        const percent = Number(raw)
        if (!WHOLE_NUMBER.test(raw) || percent < 1 || percent > 90) {
          ctx.addIssue({ code: 'custom', path: ['targetPercent'], message: 'Скидка — от 1 до 90%' })
        }
      }
    })
}

/** 10 % below today's best price, rounded to ten tenge; empty when that is not a usable target. */
export function defaultTargetPrice(best: number): string {
  const target = Math.round((best * 0.9) / 10) * 10
  return target > 0 && target < best ? String(target) : ''
}

export function defaultValues(book: BeaconBook, beacon: BeaconResponse | null): BeaconFormValues {
  const price = book.bestPrice != null ? defaultTargetPrice(book.bestPrice) : ''
  if (beacon) {
    return {
      conditionType: beacon.conditionType ?? 'ANY_DROP',
      targetPrice: beacon.targetPrice != null ? String(beacon.targetPrice) : price,
      targetPercent: beacon.targetPercent != null ? String(beacon.targetPercent) : '15',
    }
  }
  return {
    conditionType: book.bestPrice != null ? 'BELOW_PRICE' : 'ANY_DROP',
    targetPrice: price,
    targetPercent: '15',
  }
}

export function toConditionRequest(values: BeaconFormValues): {
  conditionType: ConditionType
  targetPrice?: number
  targetPercent?: number
} {
  if (values.conditionType === 'BELOW_PRICE') {
    return { conditionType: 'BELOW_PRICE', targetPrice: Number(values.targetPrice.trim()) }
  }
  if (values.conditionType === 'DISCOUNT_PERCENT') {
    return { conditionType: 'DISCOUNT_PERCENT', targetPercent: Number(values.targetPercent.trim()) }
  }
  return { conditionType: 'ANY_DROP' }
}
