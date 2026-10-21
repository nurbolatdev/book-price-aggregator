package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.search.dto.SearchItem;
import kz.nurbolat.bookaggregator.search.dto.SearchItem.BestPrice;
import kz.nurbolat.bookaggregator.search.dto.SearchItem.OfferChip;
import kz.nurbolat.bookaggregator.search.dto.SearchRequest;
import kz.nurbolat.bookaggregator.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Catalog search: at most three SQL queries per page (results, count, store chips).
 * Logging and catalog seeding run in the background.
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    /** Fewer results than this make the catalog look up the query in Google Books. */
    static final int SEED_THRESHOLD = 3;

    private final SearchRepository searchRepository;
    private final CatalogSeeder catalogSeeder;
    private final SearchLogWriter searchLogWriter;
    private final SecurityUtils securityUtils;

    public PageResponse<SearchItem> search(SearchRequest request) {
        SearchQuery query = SearchQuery.from(request);
        PageResponse<SearchItem> page = query.isBlank()
                ? PageResponse.of(List.of(), query.page(), query.size(), 0)
                : findPage(query);

        searchLogWriter.write(request.q(), query.isIsbn() ? query.isbn13() : query.text(),
                page.totalElements(), securityUtils.currentUserEmail().orElse(null));
        if (page.totalElements() < SEED_THRESHOLD && !query.isIsbn() && !query.isBlank()) {
            catalogSeeder.seed(request.q());
        }
        return page;
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
