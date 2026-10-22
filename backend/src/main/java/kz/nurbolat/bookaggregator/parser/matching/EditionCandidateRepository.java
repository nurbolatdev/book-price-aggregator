package kz.nurbolat.bookaggregator.parser.matching;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Candidate editions for an offer without ISBN: editions of the books whose normalized title is
 * most similar by pg_trgm (the {@code %} operator can use the GIN index on {@code books.title_norm}).
 */
@Repository
@RequiredArgsConstructor
class EditionCandidateRepository {

    private static final String SIMILAR_BOOKS = """
            SELECT e.id
            FROM (SELECT b.id, similarity(b.title_norm, :title) AS score
                  FROM books b
                  WHERE b.title_norm % :title
                  ORDER BY score DESC, b.id
                  LIMIT :limit) top
            JOIN editions e ON e.book_id = top.id
            ORDER BY top.score DESC, e.id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * @param titleNorm title normalized by {@code TextNormalizer}
     * @param books     how many most similar books to take
     * @return edition ids, most similar books first
     */
    List<Long> findEditionIdsOfSimilarBooks(String titleNorm, int books) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("title", titleNorm)
                .addValue("limit", books);
        return jdbc.queryForList(SIMILAR_BOOKS, params, Long.class);
    }
}
