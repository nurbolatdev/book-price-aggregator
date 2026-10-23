package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.messaging.message.ParseRequestMessage;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.StoreCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Puts crawl tasks into the queue of a store; the store's listener runs them one at a time.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParseJobPublisher {

    private final MessageSender sender;
    private final Clock clock;

    /**
     * @param editionIds editions to refresh and look for; empty for a regular crawl
     * @return {@code false} if the broker could not be reached (logged)
     */
    public boolean publish(StoreCode store, TriggerType trigger, List<Long> editionIds) {
        ParseRequestMessage message = new ParseRequestMessage(UUID.randomUUID(), store, trigger, editionIds,
                clock.instant());
        boolean sent = sender.send(RabbitTopology.parseRoutingKey(store), message.messageId(), message);
        if (sent) {
            log.info("Queued parse request {}: store={} trigger={} editions={}", message.messageId(), store, trigger,
                    message.editionIds().size());
        }
        return sent;
    }
}
