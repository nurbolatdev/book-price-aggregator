package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.messaging.message.PriceChangedMessage;
import kz.nurbolat.bookaggregator.offer.PriceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Forwards committed price changes to RabbitMQ ({@code price.changed}). Listening after commit means a
 * message leaves only for data that was really saved; a rolled back ingestion sends nothing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class PriceChangedPublisher {

    private final MessageSender sender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onPriceChanged(PriceChangedEvent event) {
        PriceChangedMessage message = new PriceChangedMessage(UUID.randomUUID(), event.offerId(), event.editionId(),
                event.store(), event.oldPrice(), event.newPrice(), event.inStock(), event.occurredAt());
        if (sender.send(RabbitTopology.PRICE_CHANGED_KEY, message.messageId(), message)) {
            log.debug("Price changed: offer={} edition={} store={} {} -> {}", event.offerId(), event.editionId(),
                    event.store(), event.oldPrice(), event.newPrice());
        }
    }
}
