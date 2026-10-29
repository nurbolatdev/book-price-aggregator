package kz.nurbolat.bookaggregator.search;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
class PopularQueriesRepository {

    private final JdbcTemplate jdbc;

    /**
     * Most searched queries since {@code since}: only searches with results, ISBN-like (digits only)
     * queries excluded. Spellings are merged by normalized text and shown as the most frequent one
     * (ties: alphabetically first).
     */
    List<String> findPopular(Instant since, int limit) {
        return jdbc.queryForList("""
                WITH variants AS (
                    SELECT query_norm, query, COUNT(*) AS cnt
                    FROM search_log
                    WHERE created_at >= ?
                      AND results_count > 0
                      AND query_norm IS NOT NULL
                      AND query_norm <> ''
                      AND query_norm !~ '^[0-9]+$'
                    GROUP BY query_norm, query
                ), totals AS (
                    SELECT query_norm, SUM(cnt) AS total FROM variants GROUP BY query_norm
                ), top_variant AS (
                    SELECT DISTINCT ON (query_norm) query_norm, query
                    FROM variants
                    ORDER BY query_norm, cnt DESC, query
                )
                SELECT v.query
                FROM top_variant v
                JOIN totals t ON t.query_norm = v.query_norm
                ORDER BY t.total DESC, v.query_norm
                LIMIT ?
                """, String.class, Timestamp.from(since), limit);
    }
}
