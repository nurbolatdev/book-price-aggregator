package kz.nurbolat.bookaggregator.home;

import kz.nurbolat.bookaggregator.home.dto.DropItem;
import kz.nurbolat.bookaggregator.store.StoreCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@RequiredArgsConstructor
class DropsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Editions in stock whose current best price is at least 1% lower than their best price at
     * {@code since} (smaller changes would show as "−0%"), biggest relative drop first. The past price of every offer is its last record before {@code since}.
     */
    List<DropItem> findDrops(Instant since, int limit) {
        String sql = """
                WITH current_best AS (
                    SELECT DISTINCT ON (o.edition_id) o.edition_id, s.code AS store_code, o.price
                    FROM offers o
                    JOIN stores s ON s.id = o.store_id
                    WHERE o.edition_id IS NOT NULL
                      AND o.match_status = 'MATCHED'
                      AND o.in_stock
                      AND s.enabled
                    ORDER BY o.edition_id, o.price
                ), past_best AS (
                    SELECT o.edition_id, MIN(past.price) AS price
                    FROM offers o
                    JOIN stores s ON s.id = o.store_id
                    CROSS JOIN LATERAL (
                        SELECT ph.price
                        FROM price_history ph
                        WHERE ph.offer_id = o.id
                          AND ph.recorded_at <= :since
                        ORDER BY ph.recorded_at DESC
                        LIMIT 1
                    ) past
                    WHERE o.edition_id IN (SELECT edition_id FROM current_best)
                      AND o.match_status = 'MATCHED'
                      AND s.enabled
                    GROUP BY o.edition_id
                )
                SELECT e.id AS edition_id, e.book_id, b.title, b.author, e.cover_url,
                       c.store_code, c.price, p.price AS previous_price
                FROM current_best c
                JOIN past_best p ON p.edition_id = c.edition_id
                JOIN editions e ON e.id = c.edition_id
                JOIN books b ON b.id = e.book_id
                WHERE c.price <= p.price * 0.99
                ORDER BY (p.price - c.price) / p.price DESC, c.edition_id
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("since", OffsetDateTime.ofInstant(since, ZoneOffset.UTC))
                .addValue("limit", limit);
        return jdbc.query(sql, params, (rs, rowNum) -> {
            BigDecimal price = rs.getBigDecimal("price");
            BigDecimal previous = rs.getBigDecimal("previous_price");
            int dropPercent = previous.subtract(price)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previous, 0, RoundingMode.HALF_UP)
                    .intValue();
            return new DropItem(rs.getLong("edition_id"), rs.getLong("book_id"), rs.getString("title"),
                    rs.getString("author"), rs.getString("cover_url"), StoreCode.valueOf(rs.getString("store_code")),
                    price, previous, dropPercent);
        });
    }
}
