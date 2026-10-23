package kz.nurbolat.bookaggregator.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
class StaleEditionRepository {

    // Editions with no matched offer seen since the given moment, including editions without offers
    private static final String STALE = """
            SELECT e.id
            FROM editions e
            WHERE e.id IN (:ids)
              AND NOT EXISTS (SELECT 1 FROM offers o
                              WHERE o.edition_id = e.id
                                AND o.match_status = 'MATCHED'
                                AND o.last_seen_at >= :since)
            ORDER BY e.id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    List<Long> findStale(Collection<Long> editionIds, Instant seenSince) {
        if (editionIds.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", editionIds)
                .addValue("since", Timestamp.from(seenSince));
        return jdbc.queryForList(STALE, params, Long.class);
    }
}
