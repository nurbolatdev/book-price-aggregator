import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { register as registerAccount } from '../api/auth'
import { ApiError, getErrorMessage } from '../api/errors'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { Checkbox } from '../components/ui/Checkbox'
import { Field, controlAttrs } from '../components/ui/Field'
import { Input } from '../components/ui/Input'
import { PasswordInput } from '../components/ui/PasswordInput'
import { AuthCard } from '../features/auth/AuthCard'
import { RateLimitAlert } from '../features/auth/RateLimitAlert'
import { authLinkClasses } from '../features/auth/authLink'
import { PASSWORD_HINT, registerSchema } from '../features/auth/schemas'
import { useRateLimit } from '../features/auth/useRateLimit'
import { useSingleSubmit } from '../features/auth/useSingleSubmit'
import { applyFieldErrors } from '../lib/applyFieldErrors'
import { formatCountdown } from '../lib/format'
import { usePageMeta } from '../lib/usePageMeta'

interface RegisterValues {
  name: string
  email: string
  password: string
  consent: boolean
}

const EMAIL_TAKEN_MESSAGE = 'Этот email уже зарегистрирован.'

export default function RegisterPage() {
  usePageMeta({
    title: 'Регистрация | KitapRadar',
    description: 'Создайте аккаунт KitapRadar, чтобы ставить маячки на цены книг.',
  })
  const navigate = useNavigate()
  const rateLimit = useRateLimit()
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    watch,
    formState: { errors },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { name: '', email: '', password: '', consent: false },
  })
  const mutation = useMutation({
    mutationFn: registerAccount,
    meta: { silentError: true },
  })
  const consent = watch('consent')

  const submit = useSingleSubmit(
    handleSubmit(async (values) => {
      setFormError(null)
      try {
        await mutation.mutateAsync({ name: values.name, email: values.email, password: values.password })
        navigate(`/check-email?email=${encodeURIComponent(values.email)}`)
      } catch (error) {
        if (rateLimit.handle(error)) return
        if (error instanceof ApiError && error.code === 'EMAIL_TAKEN') {
          setError('email', { type: 'taken', message: EMAIL_TAKEN_MESSAGE }, { shouldFocus: true })
          return
        }
        if (applyFieldErrors(error, setError, ['name', 'email', 'password'])) return
        setFormError(getErrorMessage(error))
      }
    }),
  )

  const emailError = errors.email
  const emailErrorNode = emailError ? (
    emailError.type === 'taken' ? (
      <>
        {emailError.message}{' '}
        <Link to="/login" className={`${authLinkClasses} font-semibold`}>
          Войти
        </Link>
      </>
    ) : (
      emailError.message
    )
  ) : undefined

  return (
    <AuthCard
      title="Создать аккаунт"
      description="Нужен, чтобы ставить маячки и получать письма о снижении цены."
    >
      <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-col gap-4">
        {formError && <Alert tone="danger">{formError}</Alert>}
        <RateLimitAlert rateLimit={rateLimit} />
        <Field label="Имя" error={errors.name?.message}>
          {(props) => (
            <Input
              {...controlAttrs(props)}
              inputSize="lg"
              autoComplete="name"
              {...register('name')}
            />
          )}
        </Field>
        <Field label="Email" error={emailErrorNode}>
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
        <Field label="Пароль" hint={PASSWORD_HINT} error={errors.password?.message}>
          {(props) => (
            <PasswordInput {...controlAttrs(props)} autoComplete="new-password" {...register('password')} />
          )}
        </Field>
        <Checkbox
          label="Согласен получать письма о маячках. Отписаться можно в любой момент."
          {...register('consent')}
        />
        <Button
          type="submit"
          size="form"
          className="w-full"
          loading={mutation.isPending}
          disabled={!consent || rateLimit.blocked}
        >
          {rateLimit.blocked
            ? `Создать аккаунт · ${formatCountdown(rateLimit.secondsLeft)}`
            : 'Создать аккаунт'}
        </Button>
      </form>
      <p className="text-center text-sm text-ink-soft">
        Уже есть аккаунт?{' '}
        <Link to="/login" className={`${authLinkClasses} font-semibold`}>
          Войти
        </Link>
      </p>
    </AuthCard>
  )
}
