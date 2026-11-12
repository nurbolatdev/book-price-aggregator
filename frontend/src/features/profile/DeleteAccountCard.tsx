import { useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { ApiError, getErrorMessage } from '../../api/errors'
import { deleteAccount } from '../../api/me'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Field, controlAttrs } from '../../components/ui/Field'
import { Modal } from '../../components/ui/Modal'
import { PasswordInput } from '../../components/ui/PasswordInput'
import { applyFieldErrors } from '../../lib/applyFieldErrors'
import { toast } from '../../stores/toast'
import { RateLimitAlert } from '../auth/RateLimitAlert'
import { deleteAccountSchema } from '../auth/schemas'
import { useRateLimit } from '../auth/useRateLimit'
import { useSingleSubmit } from '../auth/useSingleSubmit'

const WARNING = 'Удалим аккаунт, все маячки и историю писем. Это действие нельзя отменить.'
const ADMIN_MESSAGE = 'Аккаунт администратора удалить нельзя.'

interface Values {
  password: string
}

export function DeleteAccountCard() {
  const [open, setOpen] = useState(false)
  const triggerRef = useRef<HTMLButtonElement>(null)

  function close() {
    setOpen(false)
    triggerRef.current?.focus()
  }

  return (
    <>
      <p className="text-[15px] text-muted">{WARNING}</p>
      <Button
        ref={triggerRef}
        variant="outline-danger"
        onClick={() => setOpen(true)}
        className="w-full md:w-auto md:self-start"
      >
        Удалить аккаунт…
      </Button>
      <Modal open={open} onClose={close} title="Удалить аккаунт?">
        <DeleteForm onCancel={close} />
      </Modal>
    </>
  )
}

function DeleteForm({ onCancel }: { onCancel: () => void }) {
  const navigate = useNavigate()
  const [formError, setFormError] = useState<string | null>(null)
  const rateLimit = useRateLimit()
  const mutation = useMutation({
    mutationFn: (values: Values) => deleteAccount(values.password),
    meta: { silentError: true },
  })
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Values>({ resolver: zodResolver(deleteAccountSchema), defaultValues: { password: '' } })

  const submit = useSingleSubmit(
    handleSubmit(async (values) => {
      setFormError(null)
      try {
        await mutation.mutateAsync(values)
      } catch (error) {
        if (rateLimit.handle(error)) return
        if (error instanceof ApiError && error.status === 409) {
          setFormError(ADMIN_MESSAGE)
          return
        }
        if (applyFieldErrors(error, setError, ['password'])) return
        setFormError(getErrorMessage(error))
        return
      }
      navigate('/', { replace: true })
      toast.success('Аккаунт удалён')
    }),
  )

  return (
    <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-col gap-4">
      <p className="text-[15px] text-muted">{WARNING}</p>
      <RateLimitAlert rateLimit={rateLimit} />
      {formError && <Alert tone="danger">{formError}</Alert>}
      <Field label="Пароль" error={errors.password?.message}>
        {(props) => <PasswordInput {...controlAttrs(props)} autoComplete="current-password" {...register('password')} />}
      </Field>
      <div className="flex flex-col-reverse gap-2.5 md:flex-row md:justify-end">
        <Button variant="secondary" onClick={onCancel}>
          Отмена
        </Button>
        <Button type="submit" variant="danger" loading={mutation.isPending} disabled={rateLimit.blocked}>
          Удалить навсегда
        </Button>
      </div>
    </form>
  )
}
