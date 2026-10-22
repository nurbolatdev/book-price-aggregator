package kz.nurbolat.bookaggregator.offer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Logs committed price changes. The RabbitMQ publisher of M17 listens to the same event.
 */
@Slf4j
@Component
class PriceChangedLogListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onPriceChanged(PriceChangedEvent event) {
        log.info("Price changed: offer={} edition={} store={} {} -> {}",
                event.offerId(), event.editionId(), event.store(), event.oldPrice(), event.newPrice());
    }
}
