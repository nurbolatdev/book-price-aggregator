package kz.nurbolat.bookaggregator.messaging;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import kz.nurbolat.bookaggregator.messaging.message.ParseRequestMessage;
import kz.nurbolat.bookaggregator.offer.IngestResult;
import kz.nurbolat.bookaggregator.parser.core.CrawlResult;
import kz.nurbolat.bookaggregator.parser.core.RunStatus;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParseRequestListenerTest {

    private final StoreCrawlService crawlService = mock(StoreCrawlService.class);
    private final StoreService storeService = mock(StoreService.class);
    private final CircuitBreakerRegistry circuitBreakers = CircuitBreakerRegistry.ofDefaults();
    private final ProcessedMessages processed = spy(new ProcessedMessages(null));
    private final ParseRequestListener listener =
            new ParseRequestListener(crawlService, storeService, circuitBreakers, processed);

    private final Store flip = new Store();

    @BeforeEach
    void setUp() {
        flip.setCode(StoreCode.FLIP);
        flip.setEnabled(true);
        when(storeService.getByCode(StoreCode.FLIP)).thenReturn(flip);
        when(crawlService.hasParser(StoreCode.FLIP)).thenReturn(true);
        doReturn(false).when(processed).isProcessed(any(), any());
        doNothing().when(processed).markProcessed(any(), any());
        when(crawlService.crawl(any(), any(), any())).thenReturn(
                new CrawlResult(1L, StoreCode.FLIP, RunStatus.SUCCESS, 1, 0, IngestResult.EMPTY, null));
    }

    @Test
    void shouldCrawlRequestedEditions() {
        listener.handle(message(List.of(7L, 8L)));

        verify(crawlService).crawl(StoreCode.FLIP, TriggerType.ON_DEMAND, Optional.of(List.of(7L, 8L)));
    }

    @Test
    void shouldCrawlWithRunLimitsWhenNoEditionsAreGiven() {
        listener.handle(message(List.of()));

        verify(crawlService).crawl(StoreCode.FLIP, TriggerType.ON_DEMAND, Optional.empty());
    }

    @Test
    void shouldSkipMessageProcessedBefore() {
        doReturn(true).when(processed).isProcessed(any(), any());

        listener.handle(message(List.of()));

        verify(crawlService, never()).crawl(any(), any(), any());
    }

    @Test
    void shouldAcknowledgeWithoutCrawlingWhenStoreIsOffOrPaused() {
        flip.setEnabled(false);
        listener.handle(message(List.of()));

        flip.setEnabled(true);
        circuitBreakers.circuitBreaker("store-FLIP").transitionToOpenState();
        listener.handle(message(List.of()));

        verify(crawlService, never()).crawl(any(), any(), any());
    }

    @Test
    void shouldNotRecordMessageAndRethrowWhenCrawlFails() {
        when(crawlService.crawl(any(), any(), any())).thenThrow(new IllegalStateException("database is down"));
        ParseRequestMessage message = message(List.of());

        assertThatThrownBy(() -> listener.handle(message)).isInstanceOf(IllegalStateException.class);
        verify(processed, never()).markProcessed(ProcessedMessages.PARSE, message.messageId());
    }

    private static ParseRequestMessage message(List<Long> editionIds) {
        return new ParseRequestMessage(UUID.randomUUID(), StoreCode.FLIP, TriggerType.ON_DEMAND, editionIds,
                Instant.parse("2026-10-23T06:00:00Z"));
    }
}
