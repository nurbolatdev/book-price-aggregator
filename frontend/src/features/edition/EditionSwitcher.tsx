import { useId } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { EditionDetailResponse } from '../../api/types'
import { focusRing } from '../../components/ui/focusRing'
import { cn } from '../../lib/cn'
import { formatPrice } from '../../lib/format'
import { editionLabel } from './editionLabel'
import { usePrefetchEdition } from './usePrefetchEdition'

const base = 'min-h-touch rounded-control bg-surface px-3 text-sm'

export function EditionSwitcher({
  edition,
  editionId,
}: {
  edition: EditionDetailResponse
  editionId: number
}) {
  const labelId = useId()
  const navigate = useNavigate()
  const { state } = useLocation()
  const prefetch = usePrefetchEdition()
  const others = (edition.otherEditions ?? []).filter((o) => o.editionId != null && o.editionId !== editionId)
  if (others.length === 0) return null
  const current = editionLabel(edition) || 'Это издание'

  return (
    <div role="group" aria-labelledby={labelId} className="flex flex-col gap-2">
      <span id={labelId} className="text-sm font-semibold text-ink">
        Издание
      </span>
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          aria-pressed="true"
          className={cn(base, 'border-2 border-ink font-semibold', focusRing)}
        >
          {current}
        </button>
        {others.map((o) => {
          const id = o.editionId ?? 0
          return (
            <button
              key={id}
              type="button"
              aria-pressed="false"
              title={o.bestPrice != null ? `от ${formatPrice(o.bestPrice)}` : undefined}
              className={cn(base, 'border border-line hover:bg-canvas', focusRing)}
              onClick={() => navigate(`/editions/${id}`, { state })}
              onMouseEnter={() => prefetch(id)}
              onFocus={() => prefetch(id)}
            >
              {o.label}
            </button>
          )
        })}
      </div>
    </div>
  )
}
