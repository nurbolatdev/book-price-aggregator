package kz.nurbolat.bookaggregator.parser.core;

import kz.nurbolat.bookaggregator.edition.Binding;

import java.math.BigDecimal;

/**
 * Product data as read from a store page.
 *
 * @param externalId product id in the store, unique per store
 * @param isbn13     normalized ISBN-13, {@code null} if the page has none
 * @param oldPrice   crossed-out price, {@code null} without a discount
 */
public record ParsedOffer(
        String externalId,
        String url,
        String title,
        String author,
        String isbn13,
        BigDecimal price,
        BigDecimal oldPrice,
        boolean inStock,
        String deliveryText,
        String coverUrl,
        String publisher,
        Integer year,
        Binding binding
) {
}
