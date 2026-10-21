package kz.nurbolat.bookaggregator.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inserts an edition with one store offer and a controlled price history.
 */
public final class PriceFixtures {

    private PriceFixtures() {
    }

    /**
     * @param history pairs of (days ago, price), oldest first; the last price becomes the offer price
     * @return edition id
     */
    public static long editionWithHistory(JdbcTemplate jdbc, String title, boolean inStock, Object[]... history) {
        Long bookId = jdbc.queryForObject(
                "INSERT INTO books (title, title_norm) VALUES (?, lower(?)) RETURNING id", Long.class, title, title);
        Long editionId = jdbc.queryForObject(
                "INSERT INTO editions (book_id, publisher, year, binding) VALUES (?, 'Тест', 2024, 'PAPERBACK') RETURNING id",
                Long.class, bookId);
        addOffer(jdbc, editionId, "FLIP", inStock, history);
        return editionId;
    }

    /**
     * Adds an offer of the given store to an edition with its price history.
     */
    public static void addOffer(JdbcTemplate jdbc, long editionId, String store, boolean inStock, Object[]... history) {
        BigDecimal current = new BigDecimal(history[history.length - 1][1].toString());
        Long offerId = jdbc.queryForObject("""
                        INSERT INTO offers (edition_id, store_id, external_id, url, price, in_stock, match_status)
                        SELECT ?, id, ?, 'https://example.com/test', ?, ?, 'MATCHED' FROM stores WHERE code = ?
                        RETURNING id""",
                Long.class, editionId, "test-" + UUID.randomUUID(), current, inStock, store);
        for (Object[] point : history) {
            jdbc.update("""
                            INSERT INTO price_history (offer_id, price, recorded_at)
                            VALUES (?, ?, NOW() - make_interval(days => ?))""",
                    offerId, new BigDecimal(point[1].toString()), (Integer) point[0]);
        }
    }

    public static Object[] daysAgo(int days, int price) {
        return new Object[]{days, price};
    }
}
