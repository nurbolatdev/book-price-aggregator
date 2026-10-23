package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.messaging.ParseJobPublisher;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Every 6 hours queues a regular crawl of each switched-on store that has a parser; the store queues
 * run them at their own pace.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlScheduler {

    private final StoreService storeService;
    private final StoreCrawlService crawlService;
    private final ParseJobPublisher publisher;

    @Scheduled(cron = "${app.scheduler.crawl-cron:0 0 */6 * * *}", zone = SchedulerProperties.ZONE)
    @SchedulerLock(name = "crawl-stores", lockAtMostFor = "PT10M")
    public void queueCrawls() {
        for (Store store : storeService.findAll()) {
            if (!store.isEnabled() || !crawlService.hasParser(store.getCode())) {
                log.debug("Scheduled crawl skips {}: switched off or no parser", store.getCode());
                continue;
            }
            publisher.publish(store.getCode(), TriggerType.SCHEDULED, List.of());
        }
    }
}
