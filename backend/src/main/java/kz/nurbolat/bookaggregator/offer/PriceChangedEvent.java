package kz.nurbolat.bookaggregator.offer;

import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Price of a matched offer appeared or changed. Listeners receive it after the ingestion
 * transaction commits ({@code @TransactionalEventListener(phase = AFTER_COMMIT)}).
 *
 * @param oldPrice previous price, {@code null} for a new offer
 */
public record PriceChangedEvent(
        Long offerId,
        Long editionId,
        StoreCode store,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        Instant occurredAt
) {
}
