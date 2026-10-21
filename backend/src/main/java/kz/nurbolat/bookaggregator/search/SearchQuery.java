package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.common.text.IsbnUtils;
import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.search.dto.SearchRequest;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Search request after normalization: either an exact ISBN lookup or a fuzzy text query.
 *
 * @param text   normalized query text, {@code null} for an ISBN lookup
 * @param isbn13 normalized ISBN when the query is an ISBN
 */
record SearchQuery(
        String text,
        String isbn13,
        List<StoreCode> stores,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean inStock,
        Binding binding,
        String language,
        SearchSort sort,
        int page,
        int size
) {

    static SearchQuery from(SearchRequest request) {
        String isbn13 = IsbnUtils.normalize(request.q());
        String text = isbn13 == null ? TextNormalizer.normalize(request.q()) : null;
        return new SearchQuery(text, isbn13, request.stores(), request.minPrice(), request.maxPrice(),
                request.inStock(), request.binding(), request.language(), request.sort(),
                request.page(), request.size());
    }

    boolean isIsbn() {
        return isbn13 != null;
    }

    /** Nothing left to search for, e.g. the query consisted of punctuation only. */
    boolean isBlank() {
        return isbn13 == null && (text == null || text.isEmpty());
    }

    /** Filters that only an edition with a matching offer can satisfy. */
    boolean requiresOffer() {
        return inStock || !stores.isEmpty() || minPrice != null || maxPrice != null;
    }

    int offset() {
        return page * size;
    }
}
