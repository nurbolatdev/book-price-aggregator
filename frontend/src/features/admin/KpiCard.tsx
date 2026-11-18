import type { ReactNode } from 'react'

export function KpiCard({ label, value, caption }: { label: string; value: ReactNode; caption?: ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5 rounded-[14px] border border-line bg-surface p-4">
      <span className="text-[13px] text-muted">{label}</span>
      <span className="text-[28px] font-bold">{value}</span>
      {caption && <span className="text-[13px] text-ink-soft">{caption}</span>}
    </div>
  )
}

export function KpiGrid({ children }: { children: ReactNode }) {
  return <div className="grid grid-cols-[repeat(auto-fill,minmax(180px,1fr))] gap-3.5">{children}</div>
}
