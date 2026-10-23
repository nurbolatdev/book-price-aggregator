package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.messaging.ParseJobPublisher;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Crawl started by an administrator: checked here, then queued like any other crawl.
 */
@Service
@RequiredArgsConstructor
public class ManualRunService {

    private final StoreCrawlService crawlService;
    private final StoreService storeService;
    private final ParseJobPublisher publisher;

    /**
     * @param editionIds editions to refresh and look for; empty for a regular crawl
     * @throws NotFoundException the store has no parser
     * @throws AppException      the store is switched off ({@code CONFLICT}) or the queue is unavailable
     *                           ({@code SERVICE_UNAVAILABLE})
     */
    public void queue(StoreCode store, List<Long> editionIds) {
        if (!crawlService.hasParser(store)) {
            throw new NotFoundException("No parser for store " + store);
        }
        if (!storeService.getByCode(store).isEnabled()) {
            throw new AppException(ErrorCode.CONFLICT, "Store %s is disabled".formatted(store));
        }
        if (!publisher.publish(store, TriggerType.MANUAL, editionIds)) {
            throw new AppException(ErrorCode.SERVICE_UNAVAILABLE, "Parser queue is unavailable, try again later");
        }
    }
}
