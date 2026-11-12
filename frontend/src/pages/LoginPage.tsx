import { useId, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { login } from '../api/auth'
import { ApiError, getErrorMessage } from '../api/errors'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { Field, controlAttrs } from '../components/ui/Field'
import { Input } from '../components/ui/Input'
import { PasswordInput } from '../components/ui/PasswordInput'
import { AuthCard } from '../features/auth/AuthCard'
import { RateLimitAlert } from '../features/auth/RateLimitAlert'
import { authLinkClasses } from '../features/auth/authLink'
import { loginSchema } from '../features/auth/schemas'
import { useRateLimit } from '../features/auth/useRateLimit'
import { useSingleSubmit } from '../features/auth/useSingleSubmit'
import { writeUser } from '../features/profile/userCache'
import { applyFieldErrors } from '../lib/applyFieldErrors'
import { formatCountdown } from '../lib/format'
import { usePageMeta } from '../lib/usePageMeta'

interface LoginValues {
  email: string
  password: string
}

const INVALID_CREDENTIALS_MESSAGE =
  'Неверный email или пароль. Проверьте раскладку и попробуйте ещё раз.'

export default function LoginPage() {
  usePageMeta({
    title: 'Вход | KitapRadar',
    description: 'Войдите в KitapRadar, чтобы управлять маячками и настройками писем.',
  })
  const queryClient = useQueryClient()
  const rateLimit = useRateLimit()
  const alertId = useId()
  const [credentialsError, setCredentialsError] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    clearErrors,
    formState: { errors },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })
  const mutation = useMutation({
    mutationFn: (values: LoginValues) => login(values.email, values.password),
    meta: { silentError: true },
  })

  const clearCredentialsError = () => {
    if (!credentialsError) return
    setCredentialsError(false)
    clearErrors('email')
  }

  const submit = useSingleSubmit(
    handleSubmit(async (values) => {
      setFormError(null)
      setCredentialsError(false)
      try {
        const user = await mutation.mutateAsync(values)
        writeUser(queryClient, user)
      } catch (error) {
        if (rateLimit.handle(error)) return
        if (error instanceof ApiError && error.code === 'INVALID_CREDENTIALS') {
          setCredentialsError(true)
          setError('email', { type: 'server' }, { shouldFocus: true })
          return
        }
        if (applyFieldErrors(error, setError, ['email', 'password'])) return
        setFormError(getErrorMessage(error))
      }
    }),
  )

  const emailRegister = register('email', { onChange: clearCredentialsError })
  const passwordRegister = register('password', { onChange: clearCredentialsError })

  return (
    <AuthCard title="Вход" description="Чтобы управлять маячками и настройками писем.">
      <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-col gap-4">
        {credentialsError && (
          <div id={alertId}>
            <Alert tone="danger">{INVALID_CREDENTIALS_MESSAGE}</Alert>
          </div>
        )}
        {formError && <Alert tone="danger">{formError}</Alert>}
        <RateLimitAlert rateLimit={rateLimit} />
        <Field label="Email" error={errors.email?.message}>
          {(props) => {
            const attrs = controlAttrs(props)
            const invalid = Boolean(errors.email)
            const describedBy =
              [props.describedBy, credentialsError ? alertId : null].filter(Boolean).join(' ') ||
              undefined
            return (
              <Input
                {...attrs}
                aria-describedby={describedBy}
                aria-invalid={invalid || undefined}
                inputSize="lg"
                type="email"
                autoComplete="email"
                autoCapitalize="none"
                spellCheck={false}
                {...emailRegister}
              />
            )
          }}
        </Field>
        <Field label="Пароль" error={errors.password?.message}>
          {(props) => (
            <PasswordInput
              {...controlAttrs(props)}
              autoComplete="current-password"
              {...passwordRegister}
            />
          )}
        </Field>
        <Link to="/forgot-password" className={`${authLinkClasses} self-end text-sm font-semibold`}>
          Забыли пароль?
        </Link>
        <Button
          type="submit"
          size="form"
          className="w-full"
          loading={mutation.isPending}
          disabled={rateLimit.blocked}
        >
          {rateLimit.blocked ? `Войти · ${formatCountdown(rateLimit.secondsLeft)}` : 'Войти'}
        </Button>
      </form>
      <p className="text-center text-sm text-ink-soft">
        Нет аккаунта?{' '}
        <Link to="/register" className={`${authLinkClasses} font-semibold`}>
          Создать аккаунт
        </Link>
      </p>
    </AuthCard>
  )
}
