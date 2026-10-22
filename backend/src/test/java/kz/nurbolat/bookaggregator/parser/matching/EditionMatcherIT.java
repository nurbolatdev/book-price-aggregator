package kz.nurbolat.bookaggregator.parser.matching;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.offer.MatchStatus;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class EditionMatcherIT extends AbstractIntegrationTest {

    private static final String STRUGATSKY = "Аркадий Стругацкий, Борис Стругацкий";

    @Autowired
    private EditionMatcher matcher;

    @Autowired
    private JdbcTemplate jdbc;

    private long editionId;

    @BeforeEach
    void setUp() {
        Long bookId = jdbc.queryForObject("""
                INSERT INTO books (title, author, title_norm, author_norm)
                VALUES ('Пикник на обочине', ?, 'пикник на обочине', 'аркадий стругацкий борис стругацкий')
                RETURNING id""", Long.class, STRUGATSKY);
        editionId = jdbc.queryForObject(
                "INSERT INTO editions (book_id, publisher) VALUES (?, 'АСТ') RETURNING id", Long.class, bookId);
    }

    @Test
    void shouldMatchSimilarTitleFoundByPgTrgmWhenStoreAddsBindingAndInitials() {
        MatchResult result = matcher.match(offer("Пикник на обочине (твёрдая обложка)", "Стругацкий А. Н., Стругацкий Б. Н."));

        assertThat(result.status()).isEqualTo(MatchStatus.MATCHED);
        assertThat(result.edition().getId()).isEqualTo(editionId);
    }

    @Test
    void shouldLeaveForReviewWhenTitleIsOnlySimilar() {
        MatchResult result = matcher.match(offer("Пикник на обочине роман", STRUGATSKY));

        assertThat(result).isEqualTo(new MatchResult(MatchStatus.PENDING_REVIEW, null));
    }

    @Test
    void shouldRejectGoodsThatAreNotBooks() {
        MatchResult result = matcher.match(offer("Закладка «Пикник на обочине»", null));

        assertThat(result).isEqualTo(new MatchResult(MatchStatus.REJECTED, null));
    }

    @Test
    void shouldCreateBookAndEditionWhenNoBookIsSimilar() {
        MatchResult result = matcher.match(offer("Град обреченный (мягкая обложка)", "Аркадий Стругацкий"));

        assertThat(result.status()).isEqualTo(MatchStatus.MATCHED);
        assertThat(result.edition().getId()).isNotEqualTo(editionId);
        assertThat(jdbc.queryForObject("""
                SELECT b.title FROM books b JOIN editions e ON e.book_id = b.id WHERE e.id = ?""",
                String.class, result.edition().getId())).isEqualTo("Град обреченный");
    }

    private static ParsedOffer offer(String title, String author) {
        return new ParsedOffer("1", "https://www.flip.kz/catalog?prod=1", title, author, null,
                new BigDecimal("2500"), null, true, null, null, null, null, Binding.UNKNOWN);
    }
}
