import { Bell } from 'lucide-react'
import { useId, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import type { BeaconResponse } from '../api/types'
import { buttonClasses } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { ErrorBlock } from '../components/ui/ErrorBlock'
import { Tabs } from '../components/ui/Tabs'
import { bookFromBeacon } from '../features/beacons/beaconBook'
import { sortBeacons } from '../features/beacons/beaconFormat'
import { BeaconModal } from '../features/beacons/BeaconModal'
import { BeaconRow } from '../features/beacons/BeaconRow'
import { BeaconRowSkeleton } from '../features/beacons/BeaconRowSkeleton'
import { HowItWorksPanel } from '../features/beacons/HowItWorksPanel'
import { NotificationsPanel } from '../features/beacons/NotificationsPanel'
import { TriggeredBeaconCard } from '../features/beacons/TriggeredBeaconCard'
import { useBeacons } from '../features/beacons/useBeacons'
import { useBeaconTab } from '../features/beacons/useBeaconTab'
import { useDeleteBeacon } from '../features/beacons/useDeleteBeacon'
import { useUpdateBeacon } from '../features/beacons/useUpdateBeacon'
import { useStores } from '../features/search/useStores'
import { usePageMeta } from '../lib/usePageMeta'

export default function BeaconsPage() {
  usePageMeta({ title: 'Мои маячки | KitapRadar', description: 'Маячки на цены книг и настройки писем.' })
  const panelId = useId()
  const idPrefix = useId()
  const [tab, setTab] = useBeaconTab()
  const [editing, setEditing] = useState<BeaconResponse | null>(null)
  const query = useBeacons()
  const { storeName } = useStores()
  const update = useUpdateBeacon()
  const { remove } = useDeleteBeacon()

  const list = useMemo(() => sortBeacons(query.data ?? []), [query.data])
  const counts = useMemo(
    () => ({
      all: list.length,
      triggered: list.filter((b) => b.status === 'TRIGGERED').length,
      active: list.filter((b) => b.status === 'ACTIVE').length,
      paused: list.filter((b) => b.status === 'PAUSED').length,
    }),
    [list],
  )
  const visible = tab === 'all' ? list : list.filter((b) => b.status === tab.toUpperCase())

  const setStatus = (beacon: BeaconResponse, status: 'ACTIVE' | 'PAUSED') => {
    update.mutate({ id: beacon.id ?? 0, editionId: beacon.editionId ?? 0, patch: { status } })
  }
  const toggle = (beacon: BeaconResponse) => setStatus(beacon, beacon.status === 'PAUSED' ? 'ACTIVE' : 'PAUSED')

  const empty = query.isSuccess && list.length === 0

  let content
  if (query.isPending) {
    content = (
      <div role="status" aria-label="Загружаем маячки" className="flex flex-col gap-2.5 md:gap-[18px]">
        <BeaconRowSkeleton />
        <BeaconRowSkeleton />
        <BeaconRowSkeleton />
      </div>
    )
  } else if (query.isError) {
    content = <ErrorBlock message="Не удалось загрузить маячки." onRetry={() => void query.refetch()} />
  } else if (empty) {
    content = (
      <EmptyState
        icon={<Bell size={30} />}
        title="Маячков пока нет"
        text="Найдите книгу и нажмите «Следить за ценой» — напишем, когда она подешевеет."
        action={
          <Link to="/catalog" className={buttonClasses({ variant: 'primary' })}>
            Найти книгу
          </Link>
        }
      />
    )
  } else {
    content = (
      <>
        <Tabs
          label="Фильтр маячков"
          items={[
            { id: 'all', label: `Все · ${counts.all}` },
            { id: 'triggered', label: `Сработали · ${counts.triggered}` },
            { id: 'active', label: `Ждут · ${counts.active}` },
            { id: 'paused', label: `На паузе · ${counts.paused}` },
          ]}
          value={tab}
          onChange={setTab}
          panelId={panelId}
          idPrefix={idPrefix}
        />
        <div
          role="tabpanel"
          id={panelId}
          aria-labelledby={`${idPrefix}-${tab}`}
          className="flex flex-col gap-2.5 md:gap-[18px]"
        >
          {visible.length === 0 && <p className="text-muted">В этой вкладке пока пусто.</p>}
          {visible.map((beacon) =>
            beacon.status === 'TRIGGERED' ? (
              <TriggeredBeaconCard
                key={beacon.id}
                beacon={beacon}
                storeName={storeName}
                onFollow={(b) => setStatus(b, 'ACTIVE')}
              />
            ) : (
              <BeaconRow
                key={beacon.id}
                beacon={beacon}
                onEdit={setEditing}
                onToggle={toggle}
                onDelete={remove}
              />
            ),
          )}
        </div>
      </>
    )
  }

  return (
    <div className="mx-auto grid w-full max-w-page gap-4 px-3 pb-16 pt-5 md:gap-7 md:px-6 md:pt-8 lg:grid-cols-[minmax(0,1fr)_340px] lg:items-start">
      <div className="flex min-w-0 flex-col gap-3 md:gap-[18px]">
        <h1 className="font-display text-[26px] font-bold md:text-[30px] md:font-medium">Мои маячки</h1>
        {content}
      </div>
      <aside className="flex flex-col gap-2.5 md:gap-4">
        <NotificationsPanel />
        <HowItWorksPanel />
      </aside>
      {editing && (
        <BeaconModal open book={bookFromBeacon(editing)} beacon={editing} onClose={() => setEditing(null)} />
      )}
    </div>
  )
}
