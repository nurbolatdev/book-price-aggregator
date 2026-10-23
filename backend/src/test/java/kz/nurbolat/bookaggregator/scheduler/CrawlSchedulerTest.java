package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.messaging.ParseJobPublisher;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrawlSchedulerTest {

    private final StoreService storeService = mock(StoreService.class);
    private final StoreCrawlService crawlService = mock(StoreCrawlService.class);
    private final ParseJobPublisher publisher = mock(ParseJobPublisher.class);
    private final CrawlScheduler scheduler = new CrawlScheduler(storeService, crawlService, publisher);

    @Test
    void shouldQueueCrawlOfEverySwitchedOnStoreWithParser() {
        when(storeService.findAll()).thenReturn(List.of(
                store(StoreCode.KASPI, false), store(StoreCode.FLIP, true), store(StoreCode.OZON, true)));
        when(crawlService.hasParser(any())).thenReturn(true);

        scheduler.queueCrawls();

        verify(publisher).publish(StoreCode.FLIP, TriggerType.SCHEDULED, List.of());
        verify(publisher).publish(StoreCode.OZON, TriggerType.SCHEDULED, List.of());
        verify(publisher, never()).publish(eq(StoreCode.KASPI), any(), any());
    }

    @Test
    void shouldSkipStoreWithoutParser() {
        when(storeService.findAll()).thenReturn(List.of(store(StoreCode.WILDBERRIES, true)));
        when(crawlService.hasParser(StoreCode.WILDBERRIES)).thenReturn(false);

        scheduler.queueCrawls();

        verify(publisher, never()).publish(any(), any(), any());
    }

    private static Store store(StoreCode code, boolean enabled) {
        Store store = new Store();
        store.setCode(code);
        store.setEnabled(enabled);
        return store;
    }
}
