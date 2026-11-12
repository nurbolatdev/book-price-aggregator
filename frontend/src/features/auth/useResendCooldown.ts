import { useMutation } from '@tanstack/react-query'
import { resendVerification } from '../../api/auth'
import { ApiError, getErrorMessage } from '../../api/errors'
import { useCountdown } from '../../lib/useCountdown'
import { toast } from '../../stores/toast'
import { useRateLimit, type RateLimit } from './useRateLimit'
import { useSingleSubmit } from './useSingleSubmit'

const COOLDOWN_SECONDS = 60

export interface ResendCooldown {
  resend: (email: string) => Promise<void>
  pending: boolean
  /** Seconds until the next send is allowed (cooldown or rate limit, whichever is longer). */
  secondsLeft: number
  blocked: boolean
  rateLimit: RateLimit
}

export function useResendCooldown(options: { onRateLimited?: (seconds: number) => void } = {}): ResendCooldown {
  const cooldown = useCountdown()
  const rateLimit = useRateLimit()
  const mutation = useMutation({
    mutationFn: resendVerification,
    meta: { silentError: true },
  })

  const resend = useSingleSubmit(async (email: string) => {
    try {
      await mutation.mutateAsync(email)
      toast.success('Письмо отправлено')
      cooldown.start(COOLDOWN_SECONDS)
    } catch (error) {
      if (rateLimit.handle(error)) {
        const seconds = error instanceof ApiError ? error.retryAfter ?? 60 : 60
        options.onRateLimited?.(seconds)
      } else {
        toast.error(getErrorMessage(error))
      }
    }
  })

  const secondsLeft = Math.max(cooldown.secondsLeft, rateLimit.secondsLeft)
  return { resend, pending: mutation.isPending, secondsLeft, blocked: secondsLeft > 0, rateLimit }
}
