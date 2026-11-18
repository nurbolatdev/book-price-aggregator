import type { ReactNode } from 'react'
import { Badge, type BadgeTone } from '../../components/ui/Badge'

export function StatusBadge({ tone, children }: { tone: BadgeTone; children: ReactNode }) {
  return (
    <Badge tone={tone} className="whitespace-nowrap font-semibold">
      {children}
    </Badge>
  )
}
