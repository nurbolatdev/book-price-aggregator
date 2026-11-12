import { useSearchParams, Link } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { AuthCard } from '../features/auth/AuthCard'
import { RateLimitAlert } from '../features/auth/RateLimitAlert'
import { authLinkClasses } from '../features/auth/authLink'
import { useResendCooldown } from '../features/auth/useResendCooldown'
import { formatCountdown } from '../lib/format'
import { usePageMeta } from '../lib/usePageMeta'

export default function CheckEmailPage() {
  usePageMeta({
    title: 'Подтвердите почту | KitapRadar',
    description: 'Мы отправили письмо со ссылкой для подтверждения почты.',
  })
  const [params] = useSearchParams()
  const email = params.get('email')?.trim() || null
  const resend = useResendCooldown()

  return (
    <AuthCard icon="mail" titleSize="status" align="start" title="Подтвердите почту">
      <p className="leading-[1.6] text-ink-soft">
        Мы отправили письмо на {email ? <strong>{email}</strong> : 'указанный адрес'}. Перейдите по ссылке в
        письме — после этого маячки начнут присылать уведомления.
      </p>
      <p className="text-sm text-muted">
        Ссылка действует 24 часа. Пока почта не подтверждена, маячки можно ставить, но письма не
        отправляются.
      </p>
      {email && (
        <>
          <RateLimitAlert rateLimit={resend.rateLimit} />
          <Button
            variant="secondary"
            className="w-full"
            loading={resend.pending}
            disabled={resend.blocked}
            onClick={() => void resend.resend(email)}
          >
            {resend.blocked
              ? `Отправить письмо ещё раз · ${formatCountdown(resend.secondsLeft)}`
              : 'Отправить письмо ещё раз'}
          </Button>
        </>
      )}
      <p className="text-sm text-ink-soft">
        Указали не тот адрес?{' '}
        <Link to="/register" className={`${authLinkClasses} font-semibold`}>
          Зарегистрируйтесь заново.
        </Link>
      </p>
      <p className="text-sm text-ink-soft">
        Уже подтвердили?{' '}
        <Link to="/login" className={authLinkClasses}>
          Войти
        </Link>
      </p>
    </AuthCard>
  )
}
