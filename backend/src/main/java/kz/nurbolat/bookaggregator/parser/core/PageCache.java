package kz.nurbolat.bookaggregator.parser.core;

import java.time.Duration;
import java.util.Optional;

/**
 * Downloaded store pages, so that repeated requests for one URL do not reach the store.
 */
public interface PageCache {

    Optional<String> get(String url);

    void put(String url, String html, Duration ttl);
}
