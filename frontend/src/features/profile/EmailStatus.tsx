export function EmailStatus({ verified }: { verified: boolean }) {
  return (
    <span
      className={
        verified
          ? 'rounded-md bg-deal-soft px-2 py-0.5 text-xs font-semibold text-deal'
          : 'rounded-md bg-warn-soft px-2 py-0.5 text-xs font-semibold text-warn'
      }
    >
      {verified ? 'подтверждён' : 'не подтверждён'}
    </span>
  )
}
