import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { requeueDeadLetter } from '../../../api/admin'
import { getErrorMessage, toApiError } from '../../../api/errors'
import { queryKeys } from '../../../api/queryKeys'
import type { RequeueResponse } from '../../../api/types'
import { Alert } from '../../../components/ui/Alert'
import { Button } from '../../../components/ui/Button'
import { pluralRu } from '../../../lib/format'
import { AdminSection } from '../AdminSection'
import { ConfirmDialog } from '../ConfirmDialog'

const FORMS: [string, string, string] = ['сообщение', 'сообщения', 'сообщений']

function failureMessage(error: unknown): string {
  const apiError = toApiError(error)
  if (apiError.status === 503) return 'Очередь недоступна, попробуйте позже'
  return getErrorMessage(apiError)
}

export function DeadLetterBlock({ count }: { count: number }) {
  const queryClient = useQueryClient()
  const [confirming, setConfirming] = useState(false)
  const [result, setResult] = useState<RequeueResponse | null>(null)
  const [failure, setFailure] = useState<string | null>(null)
  const mutation = useMutation({
    mutationFn: requeueDeadLetter,
    meta: { silentError: true },
  })
  const noun = pluralRu(count, FORMS)

  async function confirm(): Promise<void> {
    setFailure(null)
    try {
      const response = await mutation.mutateAsync()
      setResult(response)
      setConfirming(false)
      await queryClient.invalidateQueries({ queryKey: queryKeys.admin.queues })
    } catch (error) {
      setResult(null)
      setFailure(failureMessage(error))
      setConfirming(false)
    }
  }

  return (
    <AdminSection
      title={`Dead letter: ${count} ${noun}`}
      className={count > 0 ? 'border-danger-line' : undefined}
    >
      <p className="text-[15px] text-ink-soft">
        Сообщения, которые не удалось обработать после повторов. Перед возвратом проверьте журнал
        событий.
      </p>
      {result && (
        <Alert tone="success">
          Возвращено {result.requeued ?? 0}, пропущено {result.skipped ?? 0}.
        </Alert>
      )}
      {failure && <Alert tone="danger">{failure}</Alert>}
      <div className="flex flex-wrap items-center gap-4">
        <Button variant="dark" size="lg" disabled={count === 0} onClick={() => setConfirming(true)}>
          Вернуть в работу
        </Button>
        <Link to="/admin/events?level=ERROR" className="text-sm font-semibold text-link underline">
          Открыть журнал событий с уровнем ERROR
        </Link>
      </div>
      <ConfirmDialog
        open={confirming}
        title="Вернуть сообщения в работу?"
        text={`${count} ${noun} снова попадут в свои очереди.`}
        confirmLabel="Вернуть в работу"
        tone="dark"
        pending={mutation.isPending}
        onConfirm={() => void confirm()}
        onClose={() => setConfirming(false)}
      />
    </AdminSection>
  )
}
