package kz.nurbolat.bookaggregator.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Token buckets in Redis (Bucket4j), shared by every application instance. Without Redis requests are let
 * through: a missing limit is better than an API that is down. After a failure Redis is not asked again for
 * {@link #RETRY_AFTER}, so that requests do not wait for it.
 */
@Slf4j
@Component
public class RateLimiter {

    private static final Duration RETRY_AFTER = Duration.ofSeconds(30);
    private static final Duration REDIS_TIMEOUT = Duration.ofMillis(500);

    private final LettuceConnectionFactory connectionFactory;
    private final Clock clock;

    private StatefulRedisConnection<String, byte[]> connection;
    private ProxyManager<String> buckets;
    private Instant unavailableUntil = Instant.MIN;

    public RateLimiter(LettuceConnectionFactory connectionFactory, Clock clock) {
        this.connectionFactory = connectionFactory;
        this.clock = clock;
    }

    /**
     * Takes one request from the bucket.
     *
     * @param key bucket name, e.g. {@code login:10.0.0.1:reader@example.com}
     * @return seconds to wait before the next request is allowed, 0 if this one is allowed
     */
    public long tryConsume(String key, RateLimitProperties.Limit limit) {
        ProxyManager<String> manager = buckets();
        if (manager == null) {
            return 0;
        }
        try {
            ConsumptionProbe probe = manager.builder()
                    .build("rate:v1:" + key, () -> configuration(limit))
                    .tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                return 0;
            }
            return Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill() + 999_999_999L));
        } catch (RuntimeException e) {
            unavailable(e);
            return 0;
        }
    }

    private static BucketConfiguration configuration(RateLimitProperties.Limit limit) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit.requests())
                        .refillIntervally(limit.requests(), limit.period())
                        .build())
                .build();
    }

    private synchronized ProxyManager<String> buckets() {
        if (clock.instant().isBefore(unavailableUntil)) {
            return null;
        }
        if (buckets != null) {
            return buckets;
        }
        try {
            RedisClient client = (RedisClient) connectionFactory.getRequiredNativeClient();
            connection = client.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
            buckets = Bucket4jLettuce.casBasedBuilder(connection)
                    .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
                    .requestTimeout(REDIS_TIMEOUT)
                    .build();
            return buckets;
        } catch (RuntimeException e) {
            unavailable(e);
            return null;
        }
    }

    private synchronized void unavailable(RuntimeException e) {
        if (clock.instant().isAfter(unavailableUntil)) {
            log.warn("Rate limits are off for {}: Redis is unavailable ({})", RETRY_AFTER, e.getMessage());
        }
        unavailableUntil = clock.instant().plus(RETRY_AFTER);
    }

    @PreDestroy
    synchronized void close() {
        if (connection != null) {
            connection.close();
        }
    }
}
