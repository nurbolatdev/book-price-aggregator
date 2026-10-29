package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.home.HomeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PopularQueriesServiceTest {

    private final PopularQueriesRepository repository = mock(PopularQueriesRepository.class);
    private final CacheManager cacheManager = mock(CacheManager.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);
    private final HomeProperties properties = new HomeProperties(List.of("Мастер и Маргарита", "Абай жолы", "1984"));

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache("popular")).thenReturn(new ConcurrentMapCache("popular"));
    }

    private PopularQueriesService service() {
        return new PopularQueriesService(repository, properties, clock, cacheManager);
    }

    @Test
    void shouldReturnOnlyLogEntriesWhenLogIsLongerThanLimit() {
        when(repository.findPopular(any(), anyInt())).thenReturn(List.of("а1", "б2", "в3", "г4"));

        assertThat(service().popular(3)).containsExactly("а1", "б2", "в3");
    }

    @Test
    void shouldPadWithFallbackAndSkipEntryEqualAfterNormalization() {
        when(repository.findPopular(any(), anyInt())).thenReturn(List.of("мастер и маргарита", "Дюна"));

        assertThat(service().popular(12))
                .containsExactly("мастер и маргарита", "Дюна", "Абай жолы", "1984");
    }

    @Test
    void shouldRespectLimitAndCacheTheTop() {
        when(repository.findPopular(any(), anyInt())).thenReturn(List.of("Дюна"));
        PopularQueriesService service = service();

        assertThat(service.popular(2)).containsExactly("Дюна", "Мастер и Маргарита");
        assertThat(service.popular(1)).containsExactly("Дюна");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(1)).findPopular(any(), anyInt());
    }
}
