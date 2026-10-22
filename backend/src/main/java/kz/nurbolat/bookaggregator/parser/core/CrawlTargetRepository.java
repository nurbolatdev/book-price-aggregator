package kz.nurbolat.bookaggregator.parser.core;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a parser run visits: known offers to refresh and editions to look up in a store.
 */
@Repository
@RequiredArgsConstructor
class CrawlTargetRepository {

    private final NamedParameterJdbcTemplate jdbc;

    record KnownOffer(Long offerId, String url) {
    }

    record EditionToFind(Long editionId, String isbn13, String title, String author, String titleNorm) {
    }

    /**
     * Offers of the store (except rejected ones) whose URL is on the store's site: editions with
     * active beacons first, then the longest unchecked. The URL check keeps foreign addresses
     * (e.g. demo data) from ever being requested.
     *
     * @param editionIds restrict to these editions, {@code null} for all
     */
    List<KnownOffer> findOffersToRefresh(Long storeId, List<Long> editionIds, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("storeId", storeId).addValue("limit", limit);
        String sql = """
                SELECT o.id, o.url
                FROM offers o
                JOIN stores s ON s.id = o.store_id
                WHERE o.store_id = :storeId
                  AND o.match_status <> 'REJECTED'
                  AND o.url LIKE s.base_url || '/%'
                """ + editionFilter("o.edition_id", editionIds, params) + """
                ORDER BY EXISTS (SELECT 1 FROM beacons b WHERE b.edition_id = o.edition_id AND b.status = 'ACTIVE') DESC,
                         o.last_seen_at ASC
                LIMIT :limit
                """;
        return jdbc.query(sql, params, (rs, rowNum) -> new KnownOffer(rs.getLong("id"), rs.getString("url")));
    }

    /**
     * Editions without an offer in the store: watched by active beacons first, then those whose
     * titles match more searches of the last 30 days.
     *
     * @param editionIds restrict to these editions, {@code null} for all
     */
    List<EditionToFind> findEditionsToDiscover(Long storeId, List<Long> editionIds, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource("storeId", storeId).addValue("limit", limit);
        String sql = """
                SELECT e.id, e.isbn13, b.title, b.author, b.title_norm
                FROM editions e
                JOIN books b ON b.id = e.book_id
                WHERE NOT EXISTS (SELECT 1 FROM offers o WHERE o.edition_id = e.id AND o.store_id = :storeId)
                """ + editionFilter("e.id", editionIds, params) + """
                ORDER BY EXISTS (SELECT 1 FROM beacons bc WHERE bc.edition_id = e.id AND bc.status = 'ACTIVE') DESC,
                         (SELECT COUNT(*)
                          FROM search_log sl
                          WHERE sl.created_at > NOW() - INTERVAL '30 days'
                            AND sl.query_norm <> ''
                            AND b.title_norm LIKE '%' || sl.query_norm || '%') DESC,
                         e.id
                LIMIT :limit
                """;
        return jdbc.query(sql, params, (rs, rowNum) -> new EditionToFind(rs.getLong("id"), rs.getString("isbn13"),
                rs.getString("title"), rs.getString("author"), rs.getString("title_norm")));
    }

    /**
     * Which of the given store product ids are already stored.
     */
    Set<String> findKnownExternalIds(Long storeId, List<String> externalIds) {
        if (externalIds.isEmpty()) {
            return Set.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource("storeId", storeId).addValue("ids", externalIds);
        return new HashSet<>(jdbc.queryForList(
                "SELECT external_id FROM offers WHERE store_id = :storeId AND external_id IN (:ids)",
                params, String.class));
    }

    private static String editionFilter(String column, List<Long> editionIds, MapSqlParameterSource params) {
        if (editionIds == null) {
            return "";
        }
        params.addValue("editionIds", editionIds.isEmpty() ? List.of(-1L) : editionIds);
        return " AND " + column + " IN (:editionIds)\n";
    }
}
