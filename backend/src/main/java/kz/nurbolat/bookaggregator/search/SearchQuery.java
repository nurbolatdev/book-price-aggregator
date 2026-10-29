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
        List<Binding> bindings,
        List<String> languages,
        SearchSort sort,
        int page,
        int size,
        boolean catalog
) {

    static SearchQuery from(SearchRequest request) {
        boolean catalog = request.q() == null;
        String isbn13 = catalog ? null : IsbnUtils.normalize(request.q());
        String text = catalog || isbn13 != null ? null : TextNormalizer.normalize(request.q());
        return new SearchQuery(text, isbn13, request.stores(), request.minPrice(), request.maxPrice(),
                request.inStock(), request.binding(), request.language(), request.sort(),
                request.page(), request.size(), catalog);
    }

    boolean isIsbn() {
        return isbn13 != null;
    }

    /** Nothing left to search for, e.g. the query consisted of punctuation only. */
    boolean isBlank() {
        return !catalog && isbn13 == null && (text == null || text.isEmpty());
    }

    /** Filters that only an edition with a matching offer can satisfy. */
    boolean requiresOffer() {
        return inStock || !stores.isEmpty() || minPrice != null || maxPrice != null;
    }

    SearchQuery withStores(List<StoreCode> value) {
        return new SearchQuery(text, isbn13, value, minPrice, maxPrice, inStock, bindings, languages, sort,
                page, size, catalog);
    }

    SearchQuery withBindings(List<Binding> value) {
        return new SearchQuery(text, isbn13, stores, minPrice, maxPrice, inStock, value, languages, sort,
                page, size, catalog);
    }

    SearchQuery withLanguages(List<String> value) {
        return new SearchQuery(text, isbn13, stores, minPrice, maxPrice, inStock, bindings, value, sort,
                page, size, catalog);
    }

    int offset() {
        return page * size;
    }
}
