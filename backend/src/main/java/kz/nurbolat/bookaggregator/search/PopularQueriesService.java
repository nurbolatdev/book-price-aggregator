package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.config.RedisConfig;
import kz.nurbolat.bookaggregator.home.HomeProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Popular queries of the last week for the search box hints. The top is cached for an hour;
 * when the log is short it is padded with the configured fallback list.
 */
@Slf4j
@Service
public class PopularQueriesService {

    static final String KEY = "top";
    static final int STORED = 12;
    private static final Duration PERIOD = Duration.ofDays(7);

    private final PopularQueriesRepository repository;
    private final HomeProperties properties;
    private final Clock clock;
    private final Cache cache;

    public PopularQueriesService(PopularQueriesRepository repository, HomeProperties properties, Clock clock,
                                 CacheManager cacheManager) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
        this.cache = Objects.requireNonNull(cacheManager.getCache(RedisConfig.POPULAR));
    }

    @SuppressWarnings("unchecked")
    public List<String> popular(int limit) {
        List<String> top = null;
        try {
            Cache.ValueWrapper cached = cache.get(KEY);
            top = cached == null ? null : (List<String>) cached.get();
        } catch (DataAccessException e) {
            log.warn("Popular queries cache unavailable: {}", e.getMessage());
        }
        if (top == null) {
            top = compute();
            try {
                cache.put(KEY, top);
            } catch (DataAccessException e) {
                log.warn("Could not store popular queries in Redis: {}", e.getMessage());
            }
        }
        return top.subList(0, Math.min(limit, top.size()));
    }

    private List<String> compute() {
        List<String> result = new ArrayList<>(repository.findPopular(clock.instant().minus(PERIOD), STORED));
        Set<String> seen = new HashSet<>();
        result.forEach(query -> seen.add(TextNormalizer.normalize(query)));
        for (String fallback : properties.popularFallback()) {
            if (result.size() >= STORED) {
                break;
            }
            if (seen.add(TextNormalizer.normalize(fallback))) {
                result.add(fallback);
            }
        }
        return result;
    }
}
