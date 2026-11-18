import { useInfiniteQuery } from "@tanstack/react-query";
import { fetchAdminEvents } from "../../../api/admin";
import { queryKeys } from "../../../api/queryKeys";
import type { EventLevel } from "../../../api/types";

export const EVENTS_PAGE_SIZE = 50;

export function useEventsJournal(level: EventLevel | undefined) {
  return useInfiniteQuery({
    queryKey: queryKeys.admin.events(level),
    queryFn: ({ pageParam, signal }) =>
      fetchAdminEvents(
        { level, page: pageParam, size: EVENTS_PAGE_SIZE },
        signal,
      ),
    initialPageParam: 0,
    getNextPageParam: (last) => {
      const page = last.page ?? 0;
      return page + 1 < (last.totalPages ?? 0) ? page + 1 : undefined;
    },
  });
}
