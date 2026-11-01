import { useStores } from '../search/useStores'

export function StoresLine() {
  const { stores, enabledStores, isError } = useStores()
  if (isError || stores.length === 0) return null
  const disabled = stores.filter((s) => !s.enabled).map((s) => s.name)

  return (
    <div className="flex flex-wrap items-center gap-x-6 gap-y-2 border-t border-line pt-3 text-[15px] text-muted max-md:gap-x-3 max-md:text-sm">
      <span className="font-semibold text-ink">Ищем в магазинах:</span>
      {enabledStores.map((s) => (
        <span key={s.code}>{s.name}</span>
      ))}
      {disabled.length > 0 && <span>{disabled.join(', ')} — временно недоступны</span>}
    </div>
  )
}
