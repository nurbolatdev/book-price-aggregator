package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class StaleEditionRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private StaleEditionRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldFindEditionsWithoutRecentlySeenMatchedOffers() {
        Instant now = Instant.now();
        long fresh = edition("Свежая");
        long old = edition("Старая");
        long empty = edition("Без предложений");
        long reviewOnly = edition("Только на проверке");
        offer(fresh, "fresh", "MATCHED", now.minus(Duration.ofHours(1)));
        offer(old, "old", "MATCHED", now.minus(Duration.ofHours(7)));
        offer(reviewOnly, "review", "PENDING_REVIEW", now.minus(Duration.ofHours(1)));

        List<Long> stale = repository.findStale(List.of(fresh, old, empty, reviewOnly), now.minus(Duration.ofHours(6)));

        assertThat(stale).containsExactlyInAnyOrder(old, empty, reviewOnly);
    }

    @Test
    void shouldReturnNothingForNoEditions() {
        assertThat(repository.findStale(List.of(), Instant.now())).isEmpty();
    }

    private long edition(String title) {
        Long bookId = jdbc.queryForObject(
                "INSERT INTO books (title, title_norm) VALUES (?, lower(?)) RETURNING id", Long.class, title, title);
        return jdbc.queryForObject("INSERT INTO editions (book_id) VALUES (?) RETURNING id", Long.class, bookId);
    }

    private void offer(long editionId, String externalId, String status, Instant seenAt) {
        jdbc.update("""
                INSERT INTO offers (edition_id, store_id, external_id, url, price, match_status,
                                    first_seen_at, last_seen_at, last_changed_at)
                SELECT ?, id, ?, 'https://www.flip.kz/catalog?prod=' || ?, 1000, ?, ?, ?, ?
                FROM stores WHERE code = 'FLIP'""", editionId, externalId, externalId, status,
                Timestamp.from(seenAt), Timestamp.from(seenAt), Timestamp.from(seenAt));
    }
}
