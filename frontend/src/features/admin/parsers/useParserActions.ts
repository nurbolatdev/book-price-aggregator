import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { runParser } from '../../../api/admin'
import { getErrorMessage, toApiError } from '../../../api/errors'
import type { AdminParser, StoreCode } from '../../../api/types'
import { toast } from '../../../stores/toast'

function rowMessage(error: unknown): string {
  const apiError = toApiError(error)
  if (apiError.status === 409) return 'Магазин выключен — сначала включите его'
  if (apiError.status === 503) return 'Очередь недоступна, попробуйте позже'
  if (apiError.status === 404) return 'У магазина нет парсера'
  return getErrorMessage(apiError)
}

export function useParserActions() {
  const mutation = useMutation({
    mutationFn: runParser,
    meta: { silentError: true },
  })
  const [rowError, setRowError] = useState<Partial<Record<StoreCode, string>>>({})
  const [running, setRunning] = useState<Set<StoreCode>>(new Set())
  const [runningAll, setRunningAll] = useState(false)

  function setRow(code: StoreCode, message: string | undefined) {
    setRowError((prev) => {
      const next = { ...prev }
      if (message === undefined) delete next[code]
      else next[code] = message
      return next
    })
  }

  function setBusy(code: StoreCode, busy: boolean) {
    setRunning((prev) => {
      const next = new Set(prev)
      if (busy) next.add(code)
      else next.delete(code)
      return next
    })
  }

  async function runOne(code: StoreCode): Promise<boolean> {
    setRow(code, undefined)
    setBusy(code, true)
    try {
      await mutation.mutateAsync(code)
      return true
    } catch (error) {
      setRow(code, rowMessage(error))
      return false
    } finally {
      setBusy(code, false)
    }
  }

  async function run(code: StoreCode): Promise<void> {
    if (await runOne(code)) toast.success('Задача в очереди')
  }

  async function runAll(parsers: AdminParser[]): Promise<void> {
    const targets = parsers.filter((p) => p.enabled && p.hasParser && p.code)
    if (targets.length === 0) {
      toast.info('Нет включённых магазинов с парсером')
      return
    }
    setRunningAll(true)
    let ok = 0
    try {
      for (const parser of targets) {
        if (await runOne(parser.code as StoreCode)) ok += 1
      }
    } finally {
      setRunningAll(false)
    }
    const message = `Поставлено в очередь: ${ok} из ${targets.length}`
    if (ok === targets.length) toast.success(message)
    else toast.error(message)
  }

  return { run, runAll, rowError, running, runningAll }
}

export type ParserActions = ReturnType<typeof useParserActions>
