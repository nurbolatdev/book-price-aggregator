import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { changePassword } from '../../api/me'
import { getErrorMessage } from '../../api/errors'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Field, controlAttrs } from '../../components/ui/Field'
import { PasswordInput } from '../../components/ui/PasswordInput'
import { applyFieldErrors } from '../../lib/applyFieldErrors'
import { formatCountdown } from '../../lib/format'
import { toast } from '../../stores/toast'
import { RateLimitAlert } from '../auth/RateLimitAlert'
import { PASSWORD_HINT, changePasswordSchema } from '../auth/schemas'
import { useRateLimit } from '../auth/useRateLimit'
import { useSingleSubmit } from '../auth/useSingleSubmit'

interface Values {
  currentPassword: string
  newPassword: string
}

export function ChangePasswordForm() {
  const [formError, setFormError] = useState<string | null>(null)
  const rateLimit = useRateLimit()
  const mutation = useMutation({
    mutationFn: (values: Values) => changePassword(values.currentPassword, values.newPassword),
    meta: { silentError: true },
  })
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '' },
  })

  const submit = useSingleSubmit(
    handleSubmit(async (values) => {
      setFormError(null)
      try {
        await mutation.mutateAsync(values)
        reset()
        toast.success('Пароль изменён, другие устройства отключены')
      } catch (error) {
        if (rateLimit.handle(error)) return
        if (applyFieldErrors(error, setError, ['currentPassword', 'newPassword'])) return
        setFormError(getErrorMessage(error))
      }
    }),
  )

  return (
    <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-col gap-4">
      <RateLimitAlert rateLimit={rateLimit} />
      {formError && <Alert tone="danger">{formError}</Alert>}
      <div className="grid gap-4 md:grid-cols-2">
        <Field label="Текущий пароль" error={errors.currentPassword?.message}>
          {(props) => (
            <PasswordInput {...controlAttrs(props)} autoComplete="current-password" {...register('currentPassword')} />
          )}
        </Field>
        <Field label="Новый пароль" hint={PASSWORD_HINT} error={errors.newPassword?.message}>
          {(props) => (
            <PasswordInput {...controlAttrs(props)} autoComplete="new-password" {...register('newPassword')} />
          )}
        </Field>
      </div>
      <Button
        type="submit"
        variant="dark"
        loading={mutation.isPending}
        disabled={rateLimit.blocked}
        className="w-full md:w-auto md:self-start"
      >
        {rateLimit.blocked ? `Сменить пароль · ${formatCountdown(rateLimit.secondsLeft)}` : 'Сменить пароль'}
      </Button>
      <p className="text-[13px] text-muted">Другие устройства будут отключены.</p>
    </form>
  )
}
