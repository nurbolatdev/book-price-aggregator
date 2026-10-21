package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Row of the search query: edition, book and the best counted offer.
 */
record SearchRow(
        Long editionId,
        Long bookId,
        String title,
        String author,
        String publisher,
        Integer year,
        Binding binding,
        String coverUrl,
        StoreCode bestStore,
        BigDecimal bestPrice,
        BigDecimal bestOldPrice,
        Boolean bestInStock,
        int storesCount,
        Instant updatedAt
) {
}
