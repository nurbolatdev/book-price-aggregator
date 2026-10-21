package kz.nurbolat.bookaggregator.edition.dto;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Edition card: the edition, its book, store offers and other editions of the same book.
 *
 * @param bestPrice cheapest in-stock offer, {@code null} when nothing is in stock
 * @param offers    in-stock offers first, then by price
 */
public record EditionDetailResponse(
        Long editionId,
        BookInfo book,
        String isbn13,
        String publisher,
        Integer year,
        Binding binding,
        String language,
        Integer pages,
        String coverUrl,
        List<OtherEdition> otherEditions,
        BestOffer bestPrice,
        PriceVerdict priceVerdict,
        List<OfferInfo> offers
) {

    public record BookInfo(Long id, String title, String author, String description) {
    }

    /**
     * @param label e.g. "Азбука 2022 · мягкий"
     */
    public record OtherEdition(Long editionId, String label, BigDecimal bestPrice) {
    }

    public record BestOffer(StoreCode store, BigDecimal price, String deliveryText, String url) {
    }

    public record OfferInfo(
            Long offerId,
            StoreCode store,
            String storeName,
            BigDecimal price,
            BigDecimal oldPrice,
            boolean inStock,
            String deliveryText,
            String url,
            Instant lastSeenAt
    ) {
    }
}
