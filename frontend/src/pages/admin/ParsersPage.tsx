import { ErrorBlock } from '../../components/ui/ErrorBlock'
import { Skeleton } from '../../components/ui/Skeleton'
import { Button } from '../../components/ui/Button'
import { AdminPageHeader } from '../../features/admin/AdminPageHeader'
import { AdminSection } from '../../features/admin/AdminSection'
import { ConfirmDialog } from '../../features/admin/ConfirmDialog'
import { ParsersTable } from '../../features/admin/parsers/ParsersTable'
import { useAdminParsers } from '../../features/admin/parsers/useAdminParsers'
import { useParserActions } from '../../features/admin/parsers/useParserActions'
import { useToggleStore } from '../../features/admin/parsers/useToggleStore'

export default function ParsersPage() {
  const parsers = useAdminParsers()
  const actions = useParserActions()
  const toggle = useToggleStore()
  const name = toggle.confirming?.name ?? ''

  return (
    <>
      <AdminPageHeader
        title="Парсеры и магазины"
        updatedAt={parsers.dataUpdatedAt}
        actions={
          <Button
            variant="dark"
            size="lg"
            loading={actions.runningAll}
            disabled={!parsers.data}
            onClick={() => parsers.data && void actions.runAll(parsers.data)}
          >
            Запустить все включённые
          </Button>
        }
      />
      <AdminSection
        title="Магазины и парсеры"
        actions={
          <p className="text-sm text-muted">
            Плановый обход: каждые 6 часов · в 00:00, 06:00, 12:00 и 18:00
          </p>
        }
      >
        {parsers.isPending && <Skeleton className="h-60 w-full" />}
        {parsers.isError && !parsers.data && <ErrorBlock onRetry={() => void parsers.refetch()} />}
        {parsers.data && (
          <ParsersTable parsers={parsers.data} variant="full" actions={actions} toggle={toggle} />
        )}
      </AdminSection>
      <ConfirmDialog
        open={toggle.confirming !== null}
        title={`Выключить ${name}?`}
        text="Магазин пропадёт из поиска и фильтров, обход остановится. Цены сохранятся."
        confirmLabel="Выключить"
        tone="danger"
        pending={toggle.pending}
        onConfirm={toggle.confirm}
        onClose={toggle.cancel}
      />
    </>
  )
}
