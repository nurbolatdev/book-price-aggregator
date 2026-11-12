import { Button } from '../../components/ui/Button'
import { formatCountdown } from '../../lib/format'
import { useResendCooldown } from '../auth/useResendCooldown'

export function EmailBanner({ email }: { email: string }) {
  const { resend, pending, secondsLeft, blocked } = useResendCooldown()
  return (
    <div
      role="status"
      className="flex flex-wrap items-center gap-3 rounded-xl bg-warn-soft px-4 py-3.5 text-warn"
    >
      <p className="flex-[1_1_320px]">Почта не подтверждена — маячки работают, но письма не отправляются.</p>
      <Button variant="secondary" loading={pending} disabled={blocked} onClick={() => void resend(email)}>
        {blocked ? `Отправить письмо ещё раз · ${formatCountdown(secondsLeft)}` : 'Отправить письмо ещё раз'}
      </Button>
    </div>
  )
}
