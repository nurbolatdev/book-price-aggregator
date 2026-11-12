import { Button } from '../../components/ui/Button'
import { formatCountdown } from '../../lib/format'
import { toast } from '../../stores/toast'
import { useResendCooldown } from '../auth/useResendCooldown'

export function EmailBanner({ email }: { email: string }) {
  const { resend, pending, secondsLeft, blocked } = useResendCooldown({
    onRateLimited: (seconds) => toast.error(`Подождите ${seconds} сек.`),
  })
  return (
    <div
      className="flex flex-wrap items-center gap-3 rounded-xl bg-warn-soft px-4 py-3.5 text-warn"
    >
      <p role="status" className="flex-[1_1_320px]">Почта не подтверждена — маячки работают, но письма не отправляются.</p>
      <Button variant="secondary" loading={pending} disabled={blocked} onClick={() => void resend(email)}>
        {blocked ? `Отправить письмо ещё раз · ${formatCountdown(secondsLeft)}` : 'Отправить письмо ещё раз'}
      </Button>
    </div>
  )
}
