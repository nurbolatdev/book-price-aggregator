package kz.nurbolat.bookaggregator.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Records every search in search_log in the background, for admin statistics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchLogWriter {

    private static final int MAX_QUERY_LENGTH = 300;

    private final JdbcTemplate jdbc;

    /**
     * @param userEmail email of the signed-in user, or {@code null}; resolved to an id inside the insert
     */
    @Async
    public void write(String query, String normalizedQuery, long resultsCount, String userEmail) {
        try {
            jdbc.update("""
                            INSERT INTO search_log (query, query_norm, results_count, user_id)
                            VALUES (?, ?, ?, (SELECT id FROM users WHERE email = ?))
                            """,
                    truncate(query), truncate(normalizedQuery), (int) Math.min(resultsCount, Integer.MAX_VALUE),
                    userEmail);
        } catch (DataAccessException e) {
            log.warn("Failed to write search log: {}", e.getMessage());
        }
    }

    private static String truncate(String value) {
        return value == null || value.length() <= MAX_QUERY_LENGTH ? value : value.substring(0, MAX_QUERY_LENGTH);
    }
}
