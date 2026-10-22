package kz.nurbolat.bookaggregator.parser.core;

import kz.nurbolat.bookaggregator.common.text.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Page cache in Redis under {@code parser:v1:html:{sha1(url)}}. Redis errors only cost a cache miss.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class RedisPageCache implements PageCache {

    private final StringRedisTemplate redis;

    @Override
    public Optional<String> get(String url) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(key(url)));
        } catch (DataAccessException e) {
            log.warn("Page cache read failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String url, String html, Duration ttl) {
        try {
            redis.opsForValue().set(key(url), html, ttl);
        } catch (DataAccessException e) {
            log.warn("Page cache write failed: {}", e.getMessage());
        }
    }

    private static String key(String url) {
        return "parser:v1:html:" + Hashing.sha1Hex(url);
    }
}
