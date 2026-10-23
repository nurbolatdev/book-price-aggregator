package kz.nurbolat.bookaggregator.messaging.message;

import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Price of a matched offer appeared or changed ({@code price.changed}), sent after the change is committed.
 *
 * @param oldPrice {@code null} when the offer is new for the edition
 */
public record PriceChangedMessage(
        UUID messageId,
        Long offerId,
        Long editionId,
        StoreCode store,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        boolean inStock,
        Instant occurredAt
) {
}
