package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.messaging.ParseJobPublisher;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RefreshOnDemandServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T06:00:00Z");

    private final StaleEditionRepository staleEditions = mock(StaleEditionRepository.class);
    private final StoreService storeService = mock(StoreService.class);
    private final StoreCrawlService crawlService = mock(StoreCrawlService.class);
    private final ParseJobPublisher publisher = mock(ParseJobPublisher.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final RefreshOnDemandService service = new RefreshOnDemandService(staleEditions, storeService,
            crawlService, publisher, redis,
            new SchedulerProperties("0 0 */6 * * *", "0 30 3 * * *", "0 0 9 * * *", "0 0 10 * * MON", Duration.ofDays(7), Duration.ofDays(90),
                    Duration.ofHours(6), Duration.ofMinutes(30)),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        when(storeService.findAll()).thenReturn(List.of(
                store(StoreCode.KASPI, false), store(StoreCode.FLIP, true), store(StoreCode.OZON, true)));
        when(crawlService.hasParser(any())).thenReturn(true);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void shouldDoNothingWhenPricesAreFresh() {
        when(staleEditions.findStale(List.of(7L), NOW.minus(Duration.ofHours(6)))).thenReturn(List.of());

        service.requestIfStale(List.of(7L));

        verifyNoInteractions(publisher);
    }

    @Test
    void shouldQueueStaleEditionsInEverySwitchedOnStore() {
        when(staleEditions.findStale(any(), any())).thenReturn(List.of(7L, 8L));

        service.requestIfStale(List.of(7L, 8L, 9L));

        verify(publisher).publish(StoreCode.FLIP, TriggerType.ON_DEMAND, List.of(7L, 8L));
        verify(publisher).publish(StoreCode.OZON, TriggerType.ON_DEMAND, List.of(7L, 8L));
        verify(publisher, never()).publish(eq(StoreCode.KASPI), any(), any());
        verify(values).setIfAbsent("parse:pending:v1:FLIP:7", "1", Duration.ofMinutes(30));
    }

    @Test
    void shouldNotQueueAgainWhileRefreshIsPending() {
        when(staleEditions.findStale(any(), any())).thenReturn(List.of(7L));
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        service.requestIfStale(List.of(7L));

        verifyNoInteractions(publisher);
    }

    @Test
    void shouldSkipQuietlyWhenRedisIsDown() {
        when(staleEditions.findStale(any(), any())).thenReturn(List.of(7L));
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        service.requestIfStale(List.of(7L));

        verifyNoInteractions(publisher);
    }

    private static Store store(StoreCode code, boolean enabled) {
        Store store = new Store();
        store.setCode(code);
        store.setEnabled(enabled);
        return store;
    }
}
