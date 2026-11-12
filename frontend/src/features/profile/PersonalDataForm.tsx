import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { getErrorMessage } from '../../api/errors'
import type { UserResponse } from '../../api/types'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Field, controlAttrs } from '../../components/ui/Field'
import { Input } from '../../components/ui/Input'
import { applyFieldErrors } from '../../lib/applyFieldErrors'
import { profileNameSchema } from '../auth/schemas'
import { useSingleSubmit } from '../auth/useSingleSubmit'
import { useUpdateProfile } from './useUpdateProfile'

interface NameValues {
  name: string
}

export function PersonalDataForm({ user }: { user: UserResponse }) {
  const [formError, setFormError] = useState<string | null>(null)
  const mutation = useUpdateProfile()
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<NameValues>({
    resolver: zodResolver(profileNameSchema),
    defaultValues: { name: user.name ?? '' },
  })

  const submit = useSingleSubmit(
    handleSubmit(async (values) => {
      setFormError(null)
      try {
        await mutation.mutateAsync(values.name)
      } catch (error) {
        if (applyFieldErrors(error, setError, ['name'])) return
        setFormError(getErrorMessage(error))
      }
    }),
  )

  return (
    <form noValidate onSubmit={(event) => void submit(event)} className="flex flex-col gap-4">
      {formError && <Alert tone="danger">{formError}</Alert>}
      <div className="grid gap-4 md:grid-cols-2">
        <Field label="Имя" error={errors.name?.message}>
          {(props) => <Input {...controlAttrs(props)} inputSize="lg" autoComplete="name" {...register('name')} />}
        </Field>
        <div className="flex flex-col gap-1.5">
          <span className="text-sm font-semibold text-ink">Email</span>
          <div className="flex min-h-12 items-center gap-2 text-base text-ink">
            <span>{user.email}</span>
            <span
              className={
                user.emailVerified
                  ? 'rounded-md bg-deal-soft px-2 py-0.5 text-xs font-semibold text-deal'
                  : 'rounded-md bg-warn-soft px-2 py-0.5 text-xs font-semibold text-warn'
              }
            >
              {user.emailVerified ? 'подтверждён' : 'не подтверждён'}
            </span>
          </div>
        </div>
      </div>
      <Button type="submit" variant="dark" loading={mutation.isPending} className="w-full md:w-auto md:self-start">
        Сохранить
      </Button>
    </form>
  )
}
