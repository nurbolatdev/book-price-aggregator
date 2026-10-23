package kz.nurbolat.bookaggregator.offer;

import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Price of a matched offer appeared or changed. Listeners receive it after the ingestion
 * transaction commits ({@code @TransactionalEventListener(phase = AFTER_COMMIT)}); the messaging
 * layer forwards it to RabbitMQ as {@code price.changed}.
 *
 * @param oldPrice previous price, {@code null} for a new offer
 * @param inStock  whether the store has the offer at the new price
 */
public record PriceChangedEvent(
        Long offerId,
        Long editionId,
        StoreCode store,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        boolean inStock,
        Instant occurredAt
) {
}
