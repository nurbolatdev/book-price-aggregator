package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.admin.dto.AdminActivityDay;
import kz.nurbolat.bookaggregator.admin.dto.AdminStatsResponse;
import kz.nurbolat.bookaggregator.config.RedisConfig;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/** Figures are compared with the baseline taken before the test rows, since the database is shared. */
@Transactional
class AdminStatsServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AdminStatsService statsService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void shouldCountUsersSearchesAndActiveUsersOfThePeriod() {
        AdminStatsResponse before = statsService.stats(StatsPeriod.H24);
        AdminStatsResponse beforeWeek = statsService.stats(StatsPeriod.D7);
        jdbc.update("""
                INSERT INTO users (email, password, email_verified, created_at)
                VALUES ('stats-new@example.com', 'x', TRUE, NOW()),
                       ('stats-old@example.com', 'x', FALSE, NOW() - INTERVAL '10 days')
                """);
        Long newId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'stats-new@example.com'", Long.class);
        jdbc.update("INSERT INTO search_log (query, query_norm, results_count, user_id) VALUES ('a', 'a', 1, ?), ('b', 'b', 0, NULL)",
                newId);
        jdbc.update("INSERT INTO search_log (query, query_norm, results_count, created_at) VALUES ('old', 'old', 1, NOW() - INTERVAL '3 days')");
        evictStatsCache();

        AdminStatsResponse after = statsService.stats(StatsPeriod.H24);

        assertThat(after.users().total() - before.users().total()).isEqualTo(2);
        assertThat(after.users().newInPeriod() - before.users().newInPeriod()).isEqualTo(1);
        assertThat(after.users().verified() - before.users().verified()).isEqualTo(1);
        assertThat(after.searches() - before.searches()).isEqualTo(2);
        assertThat(after.activeUsers() - before.activeUsers()).isEqualTo(1);
        assertThat(statsService.stats(StatsPeriod.D7).searches() - beforeWeek.searches()).isEqualTo(3);
    }

    @Test
    void shouldListEveryDayOfActivityIncludingEmptyOnes() {
        jdbc.update("INSERT INTO search_log (query, query_norm, results_count, created_at) VALUES ('x', 'x', 1, NOW() - INTERVAL '2 days')");

        List<AdminActivityDay> days = statsService.activity(5);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assertThat(days).hasSize(5);
        assertThat(days.get(0).date()).isEqualTo(today.minusDays(4));
        assertThat(days.get(4).date()).isEqualTo(today);
        assertThat(days.stream().filter(day -> day.date().equals(today.minusDays(2))).findFirst().orElseThrow()
                .searches()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldCountBeaconsEmailsAndCatalog() {
        AdminStatsResponse before = statsService.stats(StatsPeriod.H24);
        Long userId = jdbc.queryForObject("""
                INSERT INTO users (email, password, email_verified) VALUES ('stats-catalog@example.com', 'x', TRUE)
                RETURNING id""", Long.class);
        Long bookId = jdbc.queryForObject(
                "INSERT INTO books (title, title_norm) VALUES ('Stats book', 'stats book') RETURNING id", Long.class);
        Long firstEdition = jdbc.queryForObject("INSERT INTO editions (book_id) VALUES (?) RETURNING id", Long.class, bookId);
        Long secondEdition = jdbc.queryForObject("INSERT INTO editions (book_id) VALUES (?) RETURNING id", Long.class, bookId);
        jdbc.update("INSERT INTO beacons (user_id, edition_id, condition_type, status) VALUES (?, ?, 'ANY_DROP', 'ACTIVE')",
                userId, firstEdition);
        jdbc.update("""
                INSERT INTO beacons (user_id, edition_id, condition_type, status, last_triggered_at)
                VALUES (?, ?, 'ANY_DROP', 'TRIGGERED', NOW())""", userId, secondEdition);
        jdbc.update("""
                INSERT INTO notifications (user_id, status, delivery, payload, sent_at)
                VALUES (?, 'SENT', 'INSTANT', '{}'::jsonb, NOW()),
                       (?, 'SENT', 'INSTANT', '{}'::jsonb, NOW() - INTERVAL '10 days')""", userId, userId);
        jdbc.update("INSERT INTO notifications (user_id, status, delivery, payload, error) VALUES (?, 'FAILED', 'INSTANT', '{}'::jsonb, 'boom')",
                userId);
        offer(firstEdition, "stats-1", "MATCHED");
        offer(secondEdition, "stats-2", "MATCHED");
        offer(secondEdition, "stats-3", "PENDING_REVIEW");
        evictStatsCache();

        AdminStatsResponse after = statsService.stats(StatsPeriod.H24);

        assertThat(after.beacons().active() - before.beacons().active()).isEqualTo(1);
        assertThat(after.beacons().triggeredInPeriod() - before.beacons().triggeredInPeriod()).isEqualTo(1);
        assertThat(after.emails().sent() - before.emails().sent()).isEqualTo(1);
        assertThat(after.emails().failed() - before.emails().failed()).isEqualTo(1);
        assertThat(after.catalog().books() - before.catalog().books()).isEqualTo(1);
        assertThat(after.catalog().editions() - before.catalog().editions()).isEqualTo(2);
        assertThat(after.catalog().matchedOffers() - before.catalog().matchedOffers()).isEqualTo(2);
        assertThat(after.catalog().pendingReview() - before.catalog().pendingReview()).isEqualTo(1);
    }

    private void offer(long editionId, String externalId, String status) {
        jdbc.update("""
                INSERT INTO offers (edition_id, store_id, external_id, url, price, match_status,
                                    first_seen_at, last_seen_at, last_changed_at)
                SELECT ?, id, ?, 'https://www.flip.kz/catalog?prod=' || ?, 1000, ?, NOW(), NOW(), NOW()
                FROM stores WHERE code = 'FLIP'""", editionId, externalId, externalId, status);
    }

    /** Redis outlives the test transaction: a cached figure would hide the rows just inserted. */
    private void evictStatsCache() {
        Objects.requireNonNull(cacheManager.getCache(RedisConfig.ADMIN_STATS)).clear();
    }
}
