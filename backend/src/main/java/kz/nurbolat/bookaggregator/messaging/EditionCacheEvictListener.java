package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.edition.EditionCacheService;
import kz.nurbolat.bookaggregator.messaging.message.PriceChangedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Drops the cached card of an edition whose price changed, so the next view shows the new price at once
 * instead of after the 30-minute TTL. Search pages keep their 10-minute TTL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class EditionCacheEvictListener {

    private final EditionCacheService editionCacheService;
    private final ProcessedMessages processedMessages;

    @RabbitListener(id = "price-changed-cache", queues = RabbitTopology.PRICE_CHANGED_CACHE_QUEUE, concurrency = "1")
    void onPriceChanged(PriceChangedMessage message) {
        processedMessages.runOnce(message.messageId(), () -> {
            editionCacheService.evict(message.editionId());
            log.debug("Edition {} card evicted after price change of offer {}", message.editionId(), message.offerId());
        });
    }
}
