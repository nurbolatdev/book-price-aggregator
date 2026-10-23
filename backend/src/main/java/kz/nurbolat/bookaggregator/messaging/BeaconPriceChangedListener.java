package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.messaging.message.PriceChangedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumer of {@code kr.price-changed.beacons}. Only logs for now; checking beacons and queueing emails
 * arrives in M19.
 */
@Slf4j
@Component
class BeaconPriceChangedListener {

    @RabbitListener(id = "price-changed-beacons", queues = RabbitTopology.PRICE_CHANGED_BEACONS_QUEUE, concurrency = "4")
    void onPriceChanged(PriceChangedMessage message) {
        log.info("Price changed for edition {} in {}: {} -> {} (beacons are checked from M19)",
                message.editionId(), message.store(), message.oldPrice(), message.newPrice());
    }
}
