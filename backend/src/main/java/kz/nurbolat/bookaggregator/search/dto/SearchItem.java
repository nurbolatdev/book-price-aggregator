package kz.nurbolat.bookaggregator.search.dto;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * One edition in search results.
 *
 * @param bestPrice cheapest counted offer (in stock first), {@code null} if the edition has none
 * @param offers    cheapest price per store, up to five, for the store chips
 */
public record SearchItem(
        Long editionId,
        Long bookId,
        String title,
        String author,
        String publisher,
        Integer year,
        Binding binding,
        String coverUrl,
        BestPrice bestPrice,
        List<OfferChip> offers,
        int storesCount,
        Instant updatedAt
) {

    public record BestPrice(StoreCode store, BigDecimal price, BigDecimal oldPrice, boolean inStock) {
    }

    public record OfferChip(StoreCode store, BigDecimal price) {
    }
}
