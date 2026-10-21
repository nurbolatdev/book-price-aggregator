package kz.nurbolat.bookaggregator.home;

import kz.nurbolat.bookaggregator.config.RedisConfig;
import kz.nurbolat.bookaggregator.home.dto.DropItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * "Подешевели за неделю" on the home page: recalculated on a schedule and kept in Redis,
 * so the endpoint never runs the aggregation itself (except once on an empty cache).
 */
@Slf4j
@Service
public class DropsService {

    static final String KEY = "drops-week";
    static final int STORED_ITEMS = 50;
    private static final Duration PERIOD = Duration.ofDays(7);

    private final DropsRepository dropsRepository;
    private final Clock clock;
    private final Cache cache;

    public DropsService(DropsRepository dropsRepository, Clock clock, CacheManager cacheManager) {
        this.dropsRepository = dropsRepository;
        this.clock = clock;
        this.cache = Objects.requireNonNull(cacheManager.getCache(RedisConfig.DROPS));
    }

    public List<DropItem> refresh() {
        List<DropItem> drops = dropsRepository.findDrops(clock.instant().minus(PERIOD), STORED_ITEMS);
        try {
            cache.put(KEY, drops);
        } catch (DataAccessException e) {
            log.warn("Could not store weekly drops in Redis: {}", e.getMessage());
        }
        log.debug("Weekly drops recalculated: {} editions", drops.size());
        return drops;
    }

    @SuppressWarnings("unchecked")
    public List<DropItem> getDrops(int limit) {
        List<DropItem> drops = null;
        try {
            Cache.ValueWrapper cached = cache.get(KEY);
            drops = cached == null ? null : (List<DropItem>) cached.get();
        } catch (DataAccessException e) {
            log.warn("Weekly drops cache unavailable: {}", e.getMessage());
        }
        if (drops == null) {
            drops = refresh();
        }
        return drops.subList(0, Math.min(limit, drops.size()));
    }
}
