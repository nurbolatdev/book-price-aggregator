package kz.nurbolat.bookaggregator.search;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Counts of editions over the same {@code results} set as the search, so facets and search totals agree.
 */
@Repository
@RequiredArgsConstructor
class SearchFacetsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    long count(SearchQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long count = jdbc.queryForObject(
                SearchRepository.resultsSql(query, params) + " SELECT COUNT(*) FROM results", params, Long.class);
        return Objects.requireNonNullElse(count, 0L);
    }

    /** Editions per binding; the query is expected to have no binding filter. */
    Map<String, Long> countByBinding(SearchQuery query) {
        return countBy(query, "binding");
    }

    /** Editions per language; the query is expected to have no language filter. */
    Map<String, Long> countByLanguage(SearchQuery query) {
        return countBy(query, "language");
    }

    // column is one of two constants above, never user input
    private Map<String, Long> countBy(SearchQuery query, String column) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String sql = SearchRepository.resultsSql(query, params)
                + " SELECT " + column + " AS value, COUNT(*) AS total FROM results GROUP BY " + column;
        Map<String, Long> counts = new HashMap<>();
        jdbc.query(sql, params, rs -> {
            String value = rs.getString("value");
            if (value != null) {
                counts.put(value, rs.getLong("total"));
            }
        });
        return counts;
    }
}
