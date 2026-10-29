package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.edition.dto.SimilarEdition;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class SimilarEditionsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Other books of the same author first (more stores in stock, then lower price), then books with an
     * in-stock edition in the same language (more in-stock offers first). Only MATCHED in-stock offers of
     * enabled stores count; the edition shown is the cheapest one of the book.
     */
    List<SimilarEdition> findSimilar(long editionId, int limit) {
        String sql = """
                WITH cur AS (
                    SELECT e.book_id, e.language, b.author_norm
                    FROM editions e
                    JOIN books b ON b.id = e.book_id
                    WHERE e.id = :id
                ), live AS (
                    SELECT e.id AS edition_id, e.book_id, e.language, o.store_id, o.price
                    FROM offers o
                    JOIN stores s ON s.id = o.store_id
                    JOIN editions e ON e.id = o.edition_id
                    WHERE o.match_status = 'MATCHED'
                      AND o.in_stock
                      AND s.enabled
                      AND e.book_id <> (SELECT book_id FROM cur)
                ), cheapest AS (
                    SELECT DISTINCT ON (book_id) book_id, edition_id, price
                    FROM live
                    ORDER BY book_id, price, edition_id
                ), same_author AS (
                    SELECT l.book_id, COUNT(DISTINCT l.store_id) AS stores_count
                    FROM live l
                    JOIN books b ON b.id = l.book_id
                    JOIN cur ON cur.author_norm <> '' AND b.author_norm = cur.author_norm
                    GROUP BY l.book_id
                ), same_language AS (
                    SELECT l.book_id, COUNT(*) AS offers_count
                    FROM live l
                    JOIN cur ON l.language = cur.language
                    GROUP BY l.book_id
                )
                SELECT b.id AS book_id, c.edition_id, b.title, b.author, e.cover_url, c.price AS best_price
                FROM cheapest c
                JOIN books b ON b.id = c.book_id
                JOIN editions e ON e.id = c.edition_id
                LEFT JOIN same_author sa ON sa.book_id = c.book_id
                LEFT JOIN same_language sl ON sl.book_id = c.book_id
                WHERE sa.book_id IS NOT NULL OR sl.book_id IS NOT NULL
                ORDER BY (sa.book_id IS NULL), sa.stores_count DESC NULLS LAST,
                         CASE WHEN sa.book_id IS NOT NULL THEN c.price END, sl.offers_count DESC NULLS LAST, b.id
                LIMIT :limit
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("id", editionId).addValue("limit", limit);
        return jdbc.query(sql, params, (rs, rowNum) -> new SimilarEdition(
                rs.getLong("book_id"),
                rs.getLong("edition_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("cover_url"),
                rs.getBigDecimal("best_price")));
    }
}
