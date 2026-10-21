package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.book.CatalogService;
import kz.nurbolat.bookaggregator.common.text.Hashing;
import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.metadata.BookMetadata;
import kz.nurbolat.bookaggregator.metadata.GoogleBooksClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Fills the catalog with Google Books metadata (no prices) for queries that found little.
 * Runs in the background; the search response never waits for it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogSeeder {

    // The same rare query is sent to Google Books at most once per hour
    private static final Duration REPEAT_GUARD = Duration.ofHours(1);

    private final GoogleBooksClient googleBooksClient;
    private final CatalogService catalogService;
    private final StringRedisTemplate redis;

    @Async
    public void seed(String query) {
        try {
            String guardKey = "seed:v1:" + Hashing.sha1Hex(TextNormalizer.normalize(query));
            if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(guardKey, "1", REPEAT_GUARD))) {
                return;
            }
            List<BookMetadata> found = googleBooksClient.searchByText(query);
            int stored = 0;
            for (BookMetadata metadata : found) {
                if (store(metadata)) {
                    stored++;
                }
            }
            log.info("Catalog seeding for '{}': {} of {} results stored", query, stored, found.size());
        } catch (DataAccessException e) {
            log.warn("Catalog seeding for '{}' failed: {}", query, e.getMessage());
        }
    }

    private boolean store(BookMetadata metadata) {
        try {
            catalogService.addFromMetadata(metadata);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Another request stored the same ISBN in the meantime
            log.debug("Skipped duplicate edition {}", metadata.isbn13());
            return false;
        }
    }
}
