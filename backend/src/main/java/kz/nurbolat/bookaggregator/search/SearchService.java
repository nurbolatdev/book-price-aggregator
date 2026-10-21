package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.common.text.Hashing;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.config.RedisConfig;
import kz.nurbolat.bookaggregator.search.dto.SearchItem;
import kz.nurbolat.bookaggregator.search.dto.SearchItem.BestPrice;
import kz.nurbolat.bookaggregator.search.dto.SearchItem.OfferChip;
import kz.nurbolat.bookaggregator.search.dto.SearchRequest;
import kz.nurbolat.bookaggregator.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Catalog search: at most three SQL queries per page (results, count, store chips), pages cached
 * in Redis for 10 minutes. Logging and catalog seeding run in the background.
 */
@Slf4j
@Service
public class SearchService {

    /** Fewer results than this make the catalog look up the query in Google Books. */
    static final int SEED_THRESHOLD = 3;

    private final SearchRepository searchRepository;
    private final CatalogSeeder catalogSeeder;
    private final SearchLogWriter searchLogWriter;
    private final SecurityUtils securityUtils;
    private final Cache searchCache;

    public SearchService(SearchRepository searchRepository, CatalogSeeder catalogSeeder,
                         SearchLogWriter searchLogWriter, SecurityUtils securityUtils, CacheManager cacheManager) {
        this.searchRepository = searchRepository;
        this.catalogSeeder = catalogSeeder;
        this.searchLogWriter = searchLogWriter;
        this.securityUtils = securityUtils;
        this.searchCache = Objects.requireNonNull(cacheManager.getCache(RedisConfig.SEARCH));
    }

    public PageResponse<SearchItem> search(SearchRequest request) {
        SearchQuery query = SearchQuery.from(request);
        PageResponse<SearchItem> page = query.isBlank()
                ? PageResponse.of(List.of(), query.page(), query.size(), 0)
                : cachedPage(query);

        searchLogWriter.write(request.q(), query.isIsbn() ? query.isbn13() : query.text(),
                page.totalElements(), securityUtils.currentUserEmail().orElse(null));
        if (page.totalElements() < SEED_THRESHOLD && !query.isIsbn() && !query.isBlank()) {
            catalogSeeder.seed(request.q());
        }
        return page;
    }

    /**
     * Same normalized query and filters give the same key, e.g. "Мастер  и Маргарита" and
     * "мастер и маргарита".
     */
    static String cacheKey(SearchQuery query) {
        String canonical = String.join("|",
                Objects.toString(query.text(), ""),
                Objects.toString(query.isbn13(), ""),
                query.stores().stream().map(Enum::name).sorted().collect(Collectors.joining(",")),
                plain(query.minPrice()),
                plain(query.maxPrice()),
                String.valueOf(query.inStock()),
                Objects.toString(query.binding(), ""),
                Objects.toString(query.language(), ""),
                query.sort().name(),
                String.valueOf(query.page()),
                String.valueOf(query.size()));
        return Hashing.sha1Hex(canonical);
    }

    private PageResponse<SearchItem> cachedPage(SearchQuery query) {
        String key = cacheKey(query);
        try {
            return searchCache.get(key, () -> findPage(query));
        } catch (DataAccessException e) {
            // Redis is unavailable: answer from the database
            log.warn("Search cache unavailable: {}", e.getMessage());
            return findPage(query);
        }
    }

    private static String plain(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private PageResponse<SearchItem> findPage(SearchQuery query) {
        List<SearchRow> rows = searchRepository.findPage(query);
        long total = isLastPage(query, rows)
                ? (long) query.offset() + rows.size()
                : searchRepository.count(query);
        Map<Long, List<OfferChip>> chips = rows.isEmpty()
                ? Map.of()
                : searchRepository.findOfferChips(rows.stream().map(SearchRow::editionId).toList(), query);

        List<SearchItem> items = rows.stream()
                .map(row -> toItem(row, chips.getOrDefault(row.editionId(), List.of())))
                .toList();
        return PageResponse.of(items, query.page(), query.size(), total);
    }

    // A short non-empty page (or an empty first page) is the last one, so the count is known
    private static boolean isLastPage(SearchQuery query, List<SearchRow> rows) {
        return rows.size() < query.size() && (!rows.isEmpty() || query.page() == 0);
    }

    private static SearchItem toItem(SearchRow row, List<OfferChip> chips) {
        BestPrice bestPrice = row.bestPrice() == null ? null
                : new BestPrice(row.bestStore(), row.bestPrice(), row.bestOldPrice(),
                Boolean.TRUE.equals(row.bestInStock()));
        return new SearchItem(row.editionId(), row.bookId(), row.title(), row.author(), row.publisher(),
                row.year(), row.binding(), row.coverUrl(), bestPrice, chips, row.storesCount(), row.updatedAt());
    }
}
