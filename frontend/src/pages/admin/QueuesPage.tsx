import { ErrorBlock } from '../../components/ui/ErrorBlock'
import { Skeleton } from '../../components/ui/Skeleton'
import { AdminPageHeader } from '../../features/admin/AdminPageHeader'
import { AdminSection } from '../../features/admin/AdminSection'
import { DeadLetterBlock } from '../../features/admin/queues/DeadLetterBlock'
import { DEAD_LETTER } from '../../features/admin/queues/queueCaption'
import { QueuesTable } from '../../features/admin/queues/QueuesTable'
import { useAdminQueues } from '../../features/admin/overview/useOverviewQueries'

export default function QueuesPage() {
  const queues = useAdminQueues()
  const dead = queues.data?.find((q) => q.name === DEAD_LETTER)?.ready ?? 0

  return (
    <>
      <AdminPageHeader title="Очереди RabbitMQ" updatedAt={queues.dataUpdatedAt} />
      <AdminSection title="Очереди">
        {queues.isPending && <Skeleton className="h-60 w-full" />}
        {queues.isError && !queues.data && <ErrorBlock onRetry={() => void queues.refetch()} />}
        {queues.data && <QueuesTable queues={queues.data} />}
      </AdminSection>
      {queues.data && <DeadLetterBlock count={dead} />}
    </>
  )
}
