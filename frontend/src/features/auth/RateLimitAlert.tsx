import { Alert } from '../../components/ui/Alert'
import { pluralRu } from '../../lib/format'
import type { RateLimit } from './useRateLimit'

export function RateLimitAlert({ rateLimit }: { rateLimit: RateLimit }) {
  if (!rateLimit.blocked) return null
  const n = rateLimit.waitSeconds
  const unit = pluralRu(n, ['секунду', 'секунды', 'секунд'])
  return <Alert tone="warn">Слишком много попыток. Подождите {n} {unit}.</Alert>
}
