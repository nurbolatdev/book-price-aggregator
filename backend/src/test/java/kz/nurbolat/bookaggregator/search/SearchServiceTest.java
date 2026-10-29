package kz.nurbolat.bookaggregator.search;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kz.nurbolat.bookaggregator.config.RedisConfig;
import kz.nurbolat.bookaggregator.metrics.KrMetrics;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.search.dto.SearchRequest;
import kz.nurbolat.bookaggregator.security.SecurityUtils;
import kz.nurbolat.bookaggregator.store.StoreCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    private final SearchRepository repository = mock(SearchRepository.class);
    private final CatalogSeeder seeder = mock(CatalogSeeder.class);
    private final SearchLogWriter logWriter = mock(SearchLogWriter.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final CacheManager cacheManager = new ConcurrentMapCacheManager(RedisConfig.SEARCH);

    private SearchService service;

    @BeforeEach
    void setUp() {
        when(securityUtils.currentUserEmail()).thenReturn(Optional.empty());
        service = new SearchService(repository, seeder, logWriter, securityUtils, cacheManager,
                new KrMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void shouldBuildSameCacheKeyWhenQueriesDifferInCaseAndSpaces() {
        String first = SearchService.cacheKey(SearchQuery.from(request("Мастер  и Маргарита")));
        String second = SearchService.cacheKey(SearchQuery.from(request("мастер и маргарита")));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldBuildSameCacheKeyWhenStoresAreListedInAnotherOrderAndPricesHaveTrailingZeros() {
        String first = SearchService.cacheKey(SearchQuery.from(new SearchRequest("война", List.of(StoreCode.FLIP,
                StoreCode.KASPI), new BigDecimal("1000"), null, null, null, null, null, null, null)));
        String second = SearchService.cacheKey(SearchQuery.from(new SearchRequest("война", List.of(StoreCode.KASPI,
                StoreCode.FLIP), new BigDecimal("1000.00"), null, null, null, null, null, null, null)));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldBuildSameCacheKeyWhenLanguagesAndBindingsAreListedInAnotherOrder() {
        String first = SearchService.cacheKey(SearchQuery.from(new SearchRequest("война", null, null, null, null,
                List.of(Binding.HARDCOVER, Binding.PAPERBACK), List.of("ru", "kk"), null, null, null)));
        String second = SearchService.cacheKey(SearchQuery.from(new SearchRequest("война", null, null, null, null,
                List.of(Binding.PAPERBACK, Binding.HARDCOVER), List.of("kk", "ru"), null, null, null)));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldBuildDifferentCacheKeysWhenCatalogModeDiffersFromTextQuery() {
        String catalog = SearchService.cacheKey(SearchQuery.from(new SearchRequest(null, null, null, null, null,
                null, null, SearchSort.PRICE_ASC, null, null)));
        String text = SearchService.cacheKey(SearchQuery.from(new SearchRequest("война", null, null, null, null,
                null, null, SearchSort.PRICE_ASC, null, null)));

        assertThat(catalog).isNotEqualTo(text);
    }

    @Test
    void shouldBuildDifferentCacheKeysWhenFiltersOrPageDiffer() {
        String base = SearchService.cacheKey(SearchQuery.from(request("война")));
        String otherPage = SearchService.cacheKey(SearchQuery.from(
                new SearchRequest("война", null, null, null, null, null, null, null, 1, null)));
        String otherFilter = SearchService.cacheKey(SearchQuery.from(
                new SearchRequest("война", null, null, null, false, null, null, null, null, null)));

        assertThat(base).isNotEqualTo(otherPage).isNotEqualTo(otherFilter);
    }

    @Test
    void shouldSeedCatalogWhenFewerThanThreeResults() {
        when(repository.findPage(any())).thenReturn(List.of(row(1L), row(2L)));

        service.search(request("редкая книга"));

        verify(seeder).seed("редкая книга");
        verify(logWriter).write("редкая книга", "редкая книга", 2, null);
    }

    @Test
    void shouldNotSeedCatalogWhenThreeOrMoreResults() {
        when(repository.findPage(any())).thenReturn(List.of(row(1L), row(2L), row(3L)));

        service.search(request("война и мир"));

        verify(seeder, never()).seed(anyString());
    }

    @Test
    void shouldNotSeedCatalogForIsbnQueries() {
        when(repository.findPage(any())).thenReturn(Collections.emptyList());

        service.search(request("978-0-306-40615-7"));

        verify(seeder, never()).seed(anyString());
        verify(logWriter).write("978-0-306-40615-7", "9780306406157", 0, null);
    }

    @Test
    void shouldQueryDatabaseOnceForRepeatedRequest() {
        when(repository.findPage(any())).thenReturn(List.of(row(1L), row(2L), row(3L)));

        service.search(request("война и мир"));
        service.search(request("Война  и  мир"));

        verify(repository, times(1)).findPage(any());
        verify(repository, never()).count(any());
        verify(logWriter, times(2)).write(anyString(), anyString(), anyLong(), any());
    }

    private static SearchRequest request(String q) {
        return new SearchRequest(q, null, null, null, null, null, null, null, null, null);
    }

    private static SearchRow row(Long id) {
        return new SearchRow(id, id, "Книга " + id, null, null, null,
                Binding.UNKNOWN, null, null, null, null, null, 0, null);
    }
}
