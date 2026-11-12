import { useEffect, useRef } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { verifyEmail } from '../api/auth'
import { ApiError } from '../api/errors'
import { queryKeys } from '../api/queryKeys'
import { Button, buttonClasses } from '../components/ui/Button'
import { ErrorBlock } from '../components/ui/ErrorBlock'
import { Field, controlAttrs } from '../components/ui/Field'
import { Input } from '../components/ui/Input'
import { AuthCard } from '../features/auth/AuthCard'
import { RateLimitAlert } from '../features/auth/RateLimitAlert'
import { authLinkClasses } from '../features/auth/authLink'
import { emailSchema } from '../features/auth/schemas'
import { useResendCooldown } from '../features/auth/useResendCooldown'
import { writeUser } from '../features/profile/userCache'
import { formatCountdown } from '../lib/format'
import { usePageMeta } from '../lib/usePageMeta'
import { useAuthStore } from '../stores/auth'

function isServerFailure(error: unknown): boolean {
  return !(error instanceof ApiError) || error.status === 0 || error.status >= 500
}

function LinkNotWorking() {
  const ownEmail = useAuthStore((s) => s.user?.email)
  const resend = useResendCooldown()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<{ email: string }>({
    resolver: zodResolver(emailSchema),
    defaultValues: { email: '' },
  })

  const label = resend.blocked
    ? `Отправить письмо ещё раз · ${formatCountdown(resend.secondsLeft)}`
    : 'Отправить письмо ещё раз'

  return (
    <AuthCard
      icon="expired"
      titleSize="status"
      align="start"
      title="Ссылка не сработала"
      description="Ссылка уже использована или прошло больше 24 часов. Отправим новую."
    >
      <RateLimitAlert rateLimit={resend.rateLimit} />
      {ownEmail ? (
        <Button
          variant="dark"
          size="form"
          className="w-full"
          loading={resend.pending}
          disabled={resend.blocked}
          onClick={() => void resend.resend(ownEmail)}
        >
          {label}
        </Button>
      ) : (
        <form
          noValidate
          className="flex w-full flex-col gap-4"
          onSubmit={(event) => void handleSubmit((values) => resend.resend(values.email))(event)}
        >
          <Field label="Email" error={errors.email?.message}>
            {(props) => (
              <Input
                {...controlAttrs(props)}
                inputSize="lg"
                type="email"
                autoComplete="email"
                autoCapitalize="none"
                spellCheck={false}
                {...register('email')}
              />
            )}
          </Field>
          <Button
            type="submit"
            variant="dark"
            size="form"
            className="w-full"
            loading={resend.pending}
            disabled={resend.blocked}
          >
            {label}
          </Button>
        </form>
      )}
    </AuthCard>
  )
}

export default function VerifyEmailPage() {
  usePageMeta({
    title: 'Подтверждение почты | KitapRadar',
    description: 'Подтверждение адреса электронной почты.',
  })
  const [params] = useSearchParams()
  const token = params.get('token')?.trim() || null
  const queryClient = useQueryClient()
  const user = useAuthStore((s) => s.user)
  const started = useRef(false)

  const mutation = useMutation({
    mutationFn: verifyEmail,
    meta: { silentError: true },
    onSuccess: () => {
      const current = useAuthStore.getState().user
      if (current) {
        writeUser(queryClient, { ...current, emailVerified: true })
        void queryClient.invalidateQueries({ queryKey: queryKeys.me })
      }
    },
  })
  const { mutate } = mutation

  useEffect(() => {
    if (!token || started.current) return
    started.current = true
    mutate(token)
  }, [token, mutate])

  if (!token || (mutation.isError && !isServerFailure(mutation.error))) return <LinkNotWorking />

  if (mutation.isError) {
    return (
      <AuthCard titleSize="status" title="Подтверждение почты">
        <ErrorBlock message="Не удалось подтвердить почту." onRetry={() => mutate(token)} />
      </AuthCard>
    )
  }

  if (mutation.isSuccess) {
    return (
      <AuthCard icon="success" titleSize="status" align="start" title="Почта подтверждена">
        <p className="leading-[1.6] text-ink-soft">
          Готово, {user ? <strong>{user.email}</strong> : 'почта'} подтвержд
          {user ? 'ён' : 'ена'}. Теперь маячки будут присылать письма, когда цена упадёт.
        </p>
        <Link
          to="/beacons"
          className={buttonClasses({
            variant: 'primary',
            size: 'form',
            className: 'w-full',
          })}
        >
          К моим маячкам
        </Link>
        <Link to="/" className={`${authLinkClasses} text-sm font-semibold`}>
          На главную
        </Link>
      </AuthCard>
    )
  }

  return (
    <AuthCard titleSize="status" title="Подтверждение почты">
      <div role="status" className="text-muted">
        Подтверждаем почту…
      </div>
    </AuthCard>
  )
}
