package kz.nurbolat.bookaggregator.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Makes listeners idempotent: a message id is claimed in Redis ({@code processed:v1:{consumer}:{messageId}}, 24 h)
 * before processing, so a redelivered copy is skipped. The claim is per consumer: one message routed to several
 * queues (e.g. {@code price.changed} to the beacon check and to the cache) is processed by each of them. A failed attempt gives the claim back, so that
 * retries and the dead-letter queue still see the message.
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
     * Runs the work unless the message was processed before; a failure gives the message back and is rethrown.
     *
     * @param consumer name of the listener, e.g. {@link #BEACONS}
     * @return {@code false} if the message was skipped as a duplicate
     */
    public boolean runOnce(String consumer, UUID messageId, Runnable work) {
        if (!claim(consumer, messageId)) {
            return false;
        }
        try {
            work.run();
            return true;
        } catch (RuntimeException e) {
            release(consumer, messageId);
            throw e;
        }
    }

    /**
     * @return {@code false} if the message was already processed or is being processed
     */
    public boolean claim(String consumer, UUID messageId) {
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key(consumer, messageId), "1", KEEP));
        } catch (DataAccessException e) {
            // Without Redis a duplicate may run twice; losing the message would be worse
            log.warn("No duplicate check for message {}: {}", messageId, e.getMessage());
            return true;
        }
    }

    /** Called when processing failed, so that the next attempt runs. */
    public void release(String consumer, UUID messageId) {
        try {
            redis.delete(key(consumer, messageId));
        } catch (DataAccessException e) {
            log.warn("Could not release message {}: {}", messageId, e.getMessage());
        }
    }

    private static String key(String consumer, UUID messageId) {
        return "processed:v1:" + consumer + ":" + messageId;
    }
}
