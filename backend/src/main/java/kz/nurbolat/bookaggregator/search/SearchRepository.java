package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.search.dto.SearchItem.OfferChip;
import kz.nurbolat.bookaggregator.search.dto.Suggestion;
import kz.nurbolat.bookaggregator.store.StoreCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Native search over the catalog: pg_trgm on normalized titles and authors (or an exact ISBN),
 * aggregated with the matched offers of each edition. Returns projections, never entities.
 */
@Repository
@RequiredArgsConstructor
class SearchRepository {

    private static final int CHIPS_PER_EDITION = 5;

    // Text candidates: trigram similarity, word similarity (a query matching part of a title
    // or an author) and substring; all of them can use the GIN indexes on books.
    // The normalized query never contains % or _ (TextNormalizer strips punctuation).
    private static final String TEXT_CANDIDATES = """
            SELECT e.id AS edition_id,
                   GREATEST(similarity(b.title_norm, :q),
                            similarity(COALESCE(b.author_norm, ''), :q),
                            word_similarity(:q, b.title_norm),
                            word_similarity(:q, COALESCE(b.author_norm, ''))) AS relevance
            FROM books b
            JOIN editions e ON e.book_id = b.id
            WHERE b.title_norm % :q
               OR b.author_norm % :q
               OR :q <% b.title_norm
               OR :q <% b.author_norm
               OR b.title_norm LIKE :qLike
            """;

    private static final String ISBN_CANDIDATES = """
            SELECT e.id AS edition_id, 1.0::real AS relevance
            FROM editions e
            WHERE e.isbn13 = :isbn
            """;

    private final NamedParameterJdbcTemplate jdbc;

    List<SearchRow> findPage(SearchQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = resultsSql(query, params)
                + " SELECT * FROM results ORDER BY " + orderBy(query.sort())
                + " LIMIT :limit OFFSET :offset";
        params.addValue("limit", query.size());
        params.addValue("offset", query.offset());
        return jdbc.query(sql, params, SearchRepository::mapRow);
    }

    long count(SearchQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long count = jdbc.queryForObject(resultsSql(query, params) + " SELECT COUNT(*) FROM results",
                params, Long.class);
        return Objects.requireNonNullElse(count, 0L);
    }

    /**
     * Books for autocomplete: title prefix matches first, then by word similarity of title or author.
     * Books with the same normalized title and author are shown once.
     */
    List<Suggestion> findSuggestions(String normalizedQuery, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("q", normalizedQuery)
                .addValue("prefix", normalizedQuery + "%")
                .addValue("limit", limit);
        String sql = """
                SELECT id, title, author
                FROM (
                    SELECT DISTINCT ON (b.title_norm, COALESCE(b.author_norm, ''))
                           b.id, b.title, b.author,
                           b.title_norm LIKE :prefix AS prefix_match,
                           GREATEST(word_similarity(:q, b.title_norm),
                                    word_similarity(:q, COALESCE(b.author_norm, ''))) AS score
                    FROM books b
                    WHERE b.title_norm LIKE :prefix
                       OR b.title_norm % :q
                       OR :q <% b.title_norm
                       OR :q <% b.author_norm
                    ORDER BY b.title_norm, COALESCE(b.author_norm, ''), b.id
                ) unique_books
                ORDER BY prefix_match DESC, score DESC, title
                LIMIT :limit
                """;
        return jdbc.query(sql, params, (rs, rowNum) ->
                Suggestion.of(rs.getLong("id"), rs.getString("title"), rs.getString("author")));
    }

    /**
     * Cheapest price per store for the given editions, in-stock stores first, up to five per edition.
     * One query for the whole page.
     */
    Map<Long, List<OfferChip>> findOfferChips(List<Long> editionIds, SearchQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource("ids", editionIds)
                .addValue("chips", CHIPS_PER_EDITION);
        String sql = """
                SELECT edition_id, store_code, price
                FROM (
                    SELECT o.edition_id,
                           s.code AS store_code,
                           MIN(o.price) AS price,
                           ROW_NUMBER() OVER (PARTITION BY o.edition_id
                                              ORDER BY BOOL_OR(o.in_stock) DESC, MIN(o.price)) AS rn
                    FROM offers o
                    JOIN stores s ON s.id = o.store_id
                    WHERE o.edition_id IN (:ids)
                      AND o.match_status = 'MATCHED'
                      AND s.enabled
                """ + offerFilters(query, params) + """
                    GROUP BY o.edition_id, s.code
                ) ranked
                WHERE rn <= :chips
                ORDER BY edition_id, rn
                """;
        Map<Long, List<OfferChip>> chips = new LinkedHashMap<>();
        jdbc.query(sql, params, rs -> {
            chips.computeIfAbsent(rs.getLong("edition_id"), id -> new ArrayList<>())
                    .add(new OfferChip(StoreCode.valueOf(rs.getString("store_code")), rs.getBigDecimal("price")));
        });
        return chips;
    }

    /**
     * Common CTE chain ending in {@code results}: candidates, counted offers, best offer and
     * per-edition stats, with edition and price filters applied.
     */
    private static String resultsSql(SearchQuery query, MapSqlParameterSource params) {
        StringBuilder sql = new StringBuilder("WITH candidates AS (");
        if (query.isIsbn()) {
            sql.append(ISBN_CANDIDATES);
            params.addValue("isbn", query.isbn13());
        } else {
            sql.append(TEXT_CANDIDATES);
            params.addValue("q", query.text());
            params.addValue("qLike", "%" + query.text() + "%");
        }
        sql.append("""
                ), counted_offers AS (
                    SELECT o.edition_id, s.code AS store_code, o.store_id, o.price, o.old_price,
                           o.in_stock, o.last_seen_at
                    FROM offers o
                    JOIN stores s ON s.id = o.store_id
                    WHERE o.edition_id IN (SELECT edition_id FROM candidates)
                      AND o.match_status = 'MATCHED'
                      AND s.enabled
                """);
        sql.append(offerFilters(query, params));
        sql.append("""
                ), best AS (
                    SELECT DISTINCT ON (edition_id) edition_id, store_code, price, old_price, in_stock
                    FROM counted_offers
                    ORDER BY edition_id, in_stock DESC, price ASC
                ), stats AS (
                    SELECT edition_id, COUNT(DISTINCT store_id) AS stores_count, MAX(last_seen_at) AS updated_at
                    FROM counted_offers
                    GROUP BY edition_id
                ), results AS (
                    SELECT c.edition_id, c.relevance, e.book_id, b.title, b.author, e.publisher, e.year,
                           e.binding, e.cover_url,
                           bo.store_code AS best_store, bo.price AS best_price, bo.old_price AS best_old_price,
                           bo.in_stock AS best_in_stock,
                           COALESCE(st.stores_count, 0) AS stores_count, st.updated_at,
                           CASE WHEN bo.old_price > bo.price THEN 1 - bo.price / bo.old_price END AS discount
                    FROM candidates c
                    JOIN editions e ON e.id = c.edition_id
                    JOIN books b ON b.id = e.book_id
                    LEFT JOIN best bo ON bo.edition_id = c.edition_id
                    LEFT JOIN stats st ON st.edition_id = c.edition_id
                    WHERE TRUE
                """);
        if (query.requiresOffer()) {
            sql.append(" AND bo.edition_id IS NOT NULL");
        }
        if (query.binding() != null) {
            sql.append(" AND e.binding = :binding");
            params.addValue("binding", query.binding().name());
        }
        if (query.language() != null) {
            sql.append(" AND e.language = :language");
            params.addValue("language", query.language());
        }
        if (query.minPrice() != null) {
            sql.append(" AND bo.price >= :minPrice");
            params.addValue("minPrice", query.minPrice());
        }
        if (query.maxPrice() != null) {
            sql.append(" AND bo.price <= :maxPrice");
            params.addValue("maxPrice", query.maxPrice());
        }
        return sql.append(")").toString();
    }

    private static String offerFilters(SearchQuery query, MapSqlParameterSource params) {
        StringBuilder filters = new StringBuilder();
        if (query.inStock()) {
            filters.append(" AND o.in_stock\n");
        }
        if (!query.stores().isEmpty()) {
            filters.append(" AND s.code IN (:stores)\n");
            params.addValue("stores", query.stores().stream().map(Enum::name).toList());
        }
        return filters.toString();
    }

    // Edition id is the final tie-breaker so that pages never overlap
    private static String orderBy(SearchSort sort) {
        return switch (sort) {
            case RELEVANCE -> "relevance DESC, best_price ASC NULLS LAST, edition_id";
            case PRICE_ASC -> "best_price ASC NULLS LAST, relevance DESC, edition_id";
            case DISCOUNT_DESC -> "discount DESC NULLS LAST, best_price ASC NULLS LAST, edition_id";
            case STORES_DESC -> "stores_count DESC, best_price ASC NULLS LAST, edition_id";
        };
    }

    private static SearchRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        String bestStore = rs.getString("best_store");
        OffsetDateTime updatedAt = rs.getObject("updated_at", OffsetDateTime.class);
        return new SearchRow(
                rs.getLong("edition_id"),
                rs.getLong("book_id"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("publisher"),
                rs.getObject("year", Integer.class),
                Binding.valueOf(rs.getString("binding")),
                rs.getString("cover_url"),
                bestStore == null ? null : StoreCode.valueOf(bestStore),
                rs.getBigDecimal("best_price"),
                rs.getBigDecimal("best_old_price"),
                rs.getObject("best_in_stock", Boolean.class),
                rs.getInt("stores_count"),
                updatedAt == null ? null : updatedAt.toInstant()
        );
    }
}
