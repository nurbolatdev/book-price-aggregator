package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.messaging.ParseJobPublisher;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Collection;
import java.util.List;

/**
 * Stale-while-revalidate: a viewed edition whose prices were not seen in any store for 6 hours gets a crawl
 * task in every switched-on store. Runs in the background, so the page is answered from what is stored.
 * A task for the same edition and store is queued at most once in 30 minutes
 * ({@code parse:pending:v1:{store}:{editionId}}), so popular pages cannot flood the queues.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshOnDemandService {

    private final StaleEditionRepository staleEditions;
    private final StoreService storeService;
    private final StoreCrawlService crawlService;
    private final ParseJobPublisher publisher;
    private final StringRedisTemplate redis;
    private final SchedulerProperties properties;
    private final Clock clock;

    /**
     * Queues refreshes for the stale ones among the editions; never throws.
     */
    @Async
    public void requestIfStale(Collection<Long> editionIds) {
        try {
            List<Long> stale = staleEditions.findStale(editionIds, clock.instant().minus(properties.freshFor()));
            if (stale.isEmpty()) {
                return;
            }
            for (Store store : storeService.findAll()) {
                if (!store.isEnabled() || !crawlService.hasParser(store.getCode())) {
                    continue;
                }
                List<Long> notPending = stale.stream().filter(id -> markPending(store, id)).toList();
                if (!notPending.isEmpty()) {
                    publisher.publish(store.getCode(), TriggerType.ON_DEMAND, notPending);
                }
            }
        } catch (DataAccessException e) {
            log.warn("On-demand refresh of editions {} skipped: {}", editionIds, e.getMessage());
        }
    }

    private boolean markPending(Store store, Long editionId) {
        String key = "parse:pending:v1:" + store.getCode() + ":" + editionId;
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, "1", properties.pendingFor()));
    }
}
