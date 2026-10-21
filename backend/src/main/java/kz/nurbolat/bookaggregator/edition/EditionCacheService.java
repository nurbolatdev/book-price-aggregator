package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Drops a cached edition card; called when a price of the edition changes.
 */
@Service
@RequiredArgsConstructor
public class EditionCacheService {

    private final CacheManager cacheManager;

    public void evict(Long editionId) {
        Cache cache = Objects.requireNonNull(cacheManager.getCache(RedisConfig.EDITION));
        cache.evict(editionId);
    }
}
