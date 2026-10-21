package kz.nurbolat.bookaggregator.edition;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Price aggregates of editions over matched offers of enabled stores. Read-only projections.
 */
@Repository
@RequiredArgsConstructor
class EditionPriceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    record EditionPrice(Long editionId, String publisher, Integer year, Binding binding, BigDecimal bestPrice) {
    }

    /**
     * All editions of a book with their lowest in-stock price; cheapest first, editions without
     * offers last.
     */
    List<EditionPrice> findEditionsOfBook(Long bookId) {
        String sql = """
                SELECT e.id, e.publisher, e.year, e.binding,
                       (SELECT MIN(o.price)
                        FROM offers o
                        JOIN stores s ON s.id = o.store_id
                        WHERE o.edition_id = e.id
                          AND o.in_stock
                          AND o.match_status = 'MATCHED'
                          AND s.enabled) AS best_price
                FROM editions e
                WHERE e.book_id = :bookId
                ORDER BY best_price NULLS LAST, e.year DESC NULLS LAST, e.id
                """;
        return jdbc.query(sql, new MapSqlParameterSource("bookId", bookId), (rs, rowNum) -> new EditionPrice(
                rs.getLong("id"),
                rs.getString("publisher"),
                rs.getObject("year", Integer.class),
                Binding.valueOf(rs.getString("binding")),
                rs.getBigDecimal("best_price")));
    }

    /**
     * Lowest price per day in [from, to]: for every offer the last price recorded before the end
     * of the day is carried forward, so days without changes keep the previous value. Days before
     * the first record are omitted.
     */
    List<DailyPrice> findDailyMinimums(Long editionId, LocalDate from, LocalDate to) {
        String sql = """
                WITH days AS (
                    SELECT generate_series(CAST(:from AS date), CAST(:to AS date), INTERVAL '1 day')::date AS day
                ), edition_offers AS (
                    SELECT o.id
                    FROM offers o
                    JOIN stores s ON s.id = o.store_id
                    WHERE o.edition_id = :editionId
                      AND o.match_status = 'MATCHED'
                      AND s.enabled
                )
                SELECT d.day, MIN(last_price.price) AS price
                FROM days d
                CROSS JOIN edition_offers eo
                CROSS JOIN LATERAL (
                    SELECT ph.price
                    FROM price_history ph
                    WHERE ph.offer_id = eo.id
                      AND ph.recorded_at < (d.day + 1)::timestamp AT TIME ZONE 'UTC'
                    ORDER BY ph.recorded_at DESC
                    LIMIT 1
                ) last_price
                GROUP BY d.day
                ORDER BY d.day
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("editionId", editionId)
                .addValue("from", from)
                .addValue("to", to);
        return jdbc.query(sql, params, (rs, rowNum) ->
                new DailyPrice(rs.getObject("day", LocalDate.class), rs.getBigDecimal("price")));
    }
}
