package kz.nurbolat.bookaggregator.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Makes listeners idempotent: after a message was processed, its id is recorded in Redis
 * ({@code processed:v1:{consumer}:{messageId}}, 24 h), so a redelivered copy is skipped. The record is made only
 * after the work succeeded: a crash in the middle leaves no record, and the redelivered message is processed again
 * (at least once). The work itself must therefore tolerate a repeat — the database is the source of truth.
 * Records are per consumer: one message routed to several queues (e.g. {@code price.changed} to the beacon check
 * and to the cache) is processed by each of them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedMessages {

    // Consumer names in the keys: each consumer of a message processes it once
    public static final String PARSE = "parse";
    public static final String EDITION_CACHE = "edition-cache";
    public static final String EMAIL = "email";
    public static final String BEACONS = "beacons";

    private static final Duration KEEP = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    /**
     * Runs the work unless the message was processed before; a failure is rethrown and leaves no record.
     *
     * @param consumer name of the listener, e.g. {@link #BEACONS}
     * @return {@code false} if the message was skipped as a duplicate
     */
    public boolean runOnce(String consumer, UUID messageId, Runnable work) {
        if (isProcessed(consumer, messageId)) {
            return false;
        }
        work.run();
        markProcessed(consumer, messageId);
        return true;
    }

    public boolean isProcessed(String consumer, UUID messageId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(key(consumer, messageId)));
        } catch (DataAccessException e) {
            // Without Redis a duplicate may run again; losing the message would be worse
            log.warn("No duplicate check for message {}: {}", messageId, e.getMessage());
            return false;
        }
    }

    public void markProcessed(String consumer, UUID messageId) {
        try {
            redis.opsForValue().set(key(consumer, messageId), "1", KEEP);
        } catch (DataAccessException e) {
            log.warn("Could not record message {} as processed: {}", messageId, e.getMessage());
        }
    }

    private static String key(String consumer, UUID messageId) {
        return "processed:v1:" + consumer + ":" + messageId;
    }
}
