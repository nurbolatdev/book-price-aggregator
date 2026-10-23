package kz.nurbolat.bookaggregator.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Makes listeners idempotent: a message id is claimed in Redis ({@code processed:v1:{messageId}}, 24 h)
 * before processing, so a redelivered copy is skipped. A failed attempt gives the claim back, so that
 * retries and the dead-letter queue still see the message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessedMessages {

    private static final Duration KEEP = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    /**
     * Runs the work unless the message was processed before; a failure gives the message back and is rethrown.
     *
     * @return {@code false} if the message was skipped as a duplicate
     */
    public boolean runOnce(UUID messageId, Runnable work) {
        if (!claim(messageId)) {
            return false;
        }
        try {
            work.run();
            return true;
        } catch (RuntimeException e) {
            release(messageId);
            throw e;
        }
    }

    /**
     * @return {@code false} if the message was already processed or is being processed
     */
    public boolean claim(UUID messageId) {
        try {
            return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key(messageId), "1", KEEP));
        } catch (DataAccessException e) {
            // Without Redis a duplicate may run twice; losing the message would be worse
            log.warn("No duplicate check for message {}: {}", messageId, e.getMessage());
            return true;
        }
    }

    /** Called when processing failed, so that the next attempt runs. */
    public void release(UUID messageId) {
        try {
            redis.delete(key(messageId));
        } catch (DataAccessException e) {
            log.warn("Could not release message {}: {}", messageId, e.getMessage());
        }
    }

    private static String key(UUID messageId) {
        return "processed:v1:" + messageId;
    }
}
