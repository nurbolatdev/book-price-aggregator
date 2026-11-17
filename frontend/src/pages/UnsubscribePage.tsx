import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'
import { unsubscribeBeacon } from '../api/beacons'
import { ApiError } from '../api/errors'
import { Alert } from '../components/ui/Alert'
import { Button, buttonClasses } from '../components/ui/Button'
import { AuthCard } from '../features/auth/AuthCard'
import { authLinkClasses } from '../features/auth/authLink'
import { useSingleSubmit } from '../features/auth/useSingleSubmit'
import { cn } from '../lib/cn'
import { usePageMeta } from '../lib/usePageMeta'

function isLinkProblem(error: unknown): boolean {
  return error instanceof ApiError && (error.code === 'TOKEN_INVALID' || error.code === 'TOKEN_EXPIRED')
}

function LinkInvalid() {
  return (
    <AuthCard icon="expired" titleSize="status" align="start" title="Ссылка недействительна" focusTitle>
      <p className="leading-[1.6] text-ink-soft">
        Ссылка больше не действует. Войдите и поставьте маячок на паузу в «Моих маячках».
      </p>
      <Link to="/beacons" className={buttonClasses({ variant: 'dark', size: 'md' })}>
        Мои маячки
      </Link>
    </AuthCard>
  )
}

export default function UnsubscribePage() {
  usePageMeta({
    title: 'Отключение маячка | KitapRadar',
    description: 'Поставьте маячок на паузу, чтобы больше не получать письма о книге.',
  })
  const [params] = useSearchParams()
  const token = params.get('token')?.trim() || null
  const queryClient = useQueryClient()
  const [linkBroken, setLinkBroken] = useState(false)
  const [failed, setFailed] = useState(false)
  const mutation = useMutation({
    mutationFn: () => unsubscribeBeacon(token ?? ''),
    meta: { silentError: true },
  })

  const submit = useSingleSubmit(async () => {
    setFailed(false)
    try {
      await mutation.mutateAsync()
    } catch (error) {
      if (isLinkProblem(error)) setLinkBroken(true)
      else setFailed(true)
      return
    }
    void queryClient.invalidateQueries({ queryKey: ['beacons'] })
  })

  if (!token || linkBroken) return <LinkInvalid />

  if (mutation.isSuccess) {
    return (
      <AuthCard icon="paused" titleSize="status" align="start" focusTitle title="Маячок на паузе">
        <p className="leading-[1.6] text-ink-soft">
          Больше не пишем о книге «{mutation.data.title}». Остальные маячки работают как прежде.
        </p>
        <Link to="/beacons" className={buttonClasses({ variant: 'dark', size: 'md' })}>
          Открыть мои маячки
        </Link>
      </AuthCard>
    )
  }

  return (
    <AuthCard title="Отключить маячок?">
      <div className="rounded-xl bg-canvas p-3">
        <strong>Маячок из письма</strong>
      </div>
      <p className="leading-[1.6] text-ink-soft">
        Маячок встанет на паузу — писем по этой книге больше не будет. Включить его снова можно в «Моих маячках».
      </p>
      {failed && <Alert tone="danger">Не удалось поставить маячок на паузу. Попробуйте ещё раз.</Alert>}
      <Button size="form" className="w-full" loading={mutation.isPending} onClick={() => void submit()}>
        Поставить маячок на паузу
      </Button>
      <Link to="/" className={cn(authLinkClasses, 'self-center text-sm font-semibold')}>
        Оставить как есть
      </Link>
    </AuthCard>
  )
}
