package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.auth.TokenCleanupService;
import kz.nurbolat.bookaggregator.offer.OfferIngestionService;
import kz.nurbolat.bookaggregator.search.SearchLogWriter;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly cleanup: offers not seen in their store for a week are marked out of stock, search log entries
 * older than 90 days are deleted, so are refresh tokens and email links long expired.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private final StoreService storeService;
    private final OfferIngestionService offerIngestionService;
    private final SearchLogWriter searchLogWriter;
    private final TokenCleanupService tokenCleanupService;
    private final SchedulerProperties properties;

    @Scheduled(cron = "${app.scheduler.cleanup-cron:0 30 3 * * *}", zone = SchedulerProperties.ZONE)
    @SchedulerLock(name = "nightly-cleanup", lockAtMostFor = "PT30M")
    public void cleanUp() {
        int staleOffers = 0;
        for (Store store : storeService.findAll()) {
            staleOffers += offerIngestionService.markUnseenOutOfStock(store.getCode(), properties.staleOfferAfter());
        }
        int searches = searchLogWriter.deleteOlderThan(properties.searchLogKeep());
        TokenCleanupService.Result tokens = tokenCleanupService.cleanUp();
        log.info("Nightly cleanup: {} offers marked out of stock, {} search log entries, {} refresh tokens "
                        + "and {} email links deleted",
                staleOffers, searches, tokens.refreshTokens(), tokens.emailTokens());
    }
}
