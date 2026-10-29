package kz.nurbolat.bookaggregator.beacon;

import kz.nurbolat.bookaggregator.beacon.dto.BeaconResponse;
import kz.nurbolat.bookaggregator.store.StoreCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Beacons of a user with their edition and current best price in one query (no N+1).
 */
@Repository
@RequiredArgsConstructor
class BeaconListRepository {

    private static final String SELECT = """
            SELECT b.id, b.edition_id, bk.title, bk.author, e.cover_url, b.condition_type, b.target_price,
                   b.target_percent, b.status, best.price AS best_price, best.code AS best_store,
                   b.baseline_price, b.last_triggered_at, best.url AS best_url, b.created_at, b.status_changed_at
            FROM beacons b
            JOIN editions e ON e.id = b.edition_id
            JOIN books bk ON bk.id = e.book_id
            LEFT JOIN LATERAL (
                SELECT o.price, o.url, s.code
                FROM offers o
                JOIN stores s ON s.id = o.store_id
                WHERE o.edition_id = b.edition_id
                  AND o.in_stock
                  AND o.match_status = 'MATCHED'
                  AND s.enabled
                ORDER BY o.price, o.id
                LIMIT 1
            ) best ON TRUE
            WHERE b.user_id = :userId
            """;

    private static final RowMapper<BeaconResponse> MAPPER = (rs, rowNum) -> {
        Timestamp triggered = rs.getTimestamp("last_triggered_at");
        String store = rs.getString("best_store");
        return new BeaconResponse(
                rs.getLong("id"),
                rs.getLong("edition_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("cover_url"),
                BeaconCondition.valueOf(rs.getString("condition_type")),
                rs.getBigDecimal("target_price"),
                rs.getObject("target_percent", Integer.class),
                BeaconStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("best_price"),
                store == null ? null : StoreCode.valueOf(store),
                rs.getString("best_url"),
                rs.getBigDecimal("baseline_price"),
                triggered == null ? null : triggered.toInstant(),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("status_changed_at").toInstant());
    };

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Triggered first (latest first), then the rest newest first.
     *
     * @param status    {@code null} for all
     * @param editionId {@code null} for all editions
     */
    List<BeaconResponse> findOfUser(Long userId, BeaconStatus status, Long editionId) {
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        String sql = SELECT;
        if (status != null) {
            sql += " AND b.status = :status";
            params.addValue("status", status.name());
        }
        if (editionId != null) {
            sql += " AND b.edition_id = :editionId";
            params.addValue("editionId", editionId);
        }
        sql += " ORDER BY (b.status = 'TRIGGERED') DESC, COALESCE(b.last_triggered_at, b.created_at) DESC, b.id DESC";
        return jdbc.query(sql, params, MAPPER);
    }

    Optional<BeaconResponse> findOne(Long userId, Long beaconId) {
        return jdbc.query(SELECT + " AND b.id = :id",
                new MapSqlParameterSource("userId", userId).addValue("id", beaconId), MAPPER).stream().findFirst();
    }
}
