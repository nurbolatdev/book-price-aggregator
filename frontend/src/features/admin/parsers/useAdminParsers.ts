import { useQuery } from '@tanstack/react-query'
import { fetchAdminParsers } from '../../../api/admin'
import { queryKeys } from '../../../api/queryKeys'
import { ADMIN_POLL } from '../polling'

export function useAdminParsers() {
  return useQuery({
    queryKey: queryKeys.admin.parsers,
    queryFn: ({ signal }) => fetchAdminParsers(signal),
    ...ADMIN_POLL,
  })
}
