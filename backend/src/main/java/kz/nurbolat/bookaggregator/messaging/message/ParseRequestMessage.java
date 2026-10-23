package kz.nurbolat.bookaggregator.messaging.message;

import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Task to crawl one store ({@code parse.request.<store>}).
 *
 * @param editionIds editions to refresh and look for; empty for a regular crawl within the run limits
 */
public record ParseRequestMessage(
        UUID messageId,
        StoreCode store,
        TriggerType trigger,
        List<Long> editionIds,
        Instant occurredAt
) {

    public ParseRequestMessage {
        editionIds = editionIds == null ? List.of() : List.copyOf(editionIds);
    }
}
