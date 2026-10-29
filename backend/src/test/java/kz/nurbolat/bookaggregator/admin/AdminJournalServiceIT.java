package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.admin.dto.AdminEventResponse;
import kz.nurbolat.bookaggregator.admin.dto.AdminUserResponse;
import kz.nurbolat.bookaggregator.admin.dto.SearchQueryStat;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Seeded rows are dated in the future, so they come first whatever else the shared database holds. */
@Transactional
class AdminJournalServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AdminJournalService service;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldListEventsNewestFirst() {
        event("INFO", "journal-a", "1 day");
        event("ERROR", "journal-b", "2 days");
        event("WARN", "journal-c", "3 days");

        PageResponse<AdminEventResponse> page = service.events(null, 0, 3);

        assertThat(page.content()).extracting(AdminEventResponse::message)
                .containsExactly("journal-c", "journal-b", "journal-a");
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldFilterEventsByLevel() {
        event("INFO", "journal-info", "1 day");
        event("ERROR", "journal-error-old", "2 days");
        event("ERROR", "journal-error-new", "3 days");

        PageResponse<AdminEventResponse> errors = service.events(SystemEvent.Level.ERROR, 0, 2);

        assertThat(errors.content()).extracting(AdminEventResponse::message)
                .containsExactly("journal-error-new", "journal-error-old");
        assertThat(errors.content()).allMatch(event -> event.level() == SystemEvent.Level.ERROR);
    }

    @Test
    void shouldPageEvents() {
        event("INFO", "journal-p1", "1 day");
        event("INFO", "journal-p2", "2 days");
        event("INFO", "journal-p3", "3 days");

        assertThat(service.events(null, 0, 1).content()).extracting(AdminEventResponse::message)
                .containsExactly("journal-p3");
        assertThat(service.events(null, 1, 1).content()).extracting(AdminEventResponse::message)
                .containsExactly("journal-p2");
    }

    @Test
    void shouldRankTopQueriesByCountWithinPeriod() {
        search("zz-top-popular", 3, 5, "1 hour");
        search("zz-top-rare", 1, 5, "1 hour");
        search("zz-top-old", 4, 5, "3 days");

        List<SearchQueryStat> day = service.topQueries(StatsPeriod.H24);
        List<SearchQueryStat> week = service.topQueries(StatsPeriod.D7);

        assertThat(count(day, "zz-top-popular")).isEqualTo(3);
        assertThat(count(day, "zz-top-rare")).isEqualTo(1);
        assertThat(count(day, "zz-top-old")).isNull();
        assertThat(count(week, "zz-top-old")).isEqualTo(4);
        assertThat(day.size()).isLessThanOrEqualTo(AdminJournalService.TOP_LIMIT);
        assertThat(day).extracting(SearchQueryStat::searches).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void shouldListOnlyQueriesWithoutResultsAsZeroResults() {
        search("zz-zero-miss", 2, 0, "1 hour");
        search("zz-zero-hit", 5, 7, "1 hour");
        search("zz-zero-old", 1, 0, "10 days");

        List<SearchQueryStat> zero = service.zeroResultQueries(StatsPeriod.D7);

        assertThat(count(zero, "zz-zero-miss")).isEqualTo(2);
        assertThat(count(zero, "zz-zero-hit")).isNull();
        assertThat(count(zero, "zz-zero-old")).isNull();
        assertThat(count(service.zeroResultQueries(StatsPeriod.D30), "zz-zero-old")).isEqualTo(1);
    }

    @Test
    void shouldListUsersNewestFirstWithBeaconCount() {
        long first = user("journal-1@example.com", "Ann", true, "1 day");
        long second = user("journal-2@example.com", null, false, "2 days");
        long edition = edition();
        beacon(first, edition);

        PageResponse<AdminUserResponse> page = service.users(null, null, 0, 2);

        assertThat(page.content()).extracting(AdminUserResponse::email)
                .containsExactly("journal-2@example.com", "journal-1@example.com");
        AdminUserResponse older = page.content().get(1);
        assertThat(older.id()).isEqualTo(first);
        assertThat(older.name()).isEqualTo("Ann");
        assertThat(older.verified()).isTrue();
        assertThat(older.beacons()).isEqualTo(1);
        assertThat(page.content().get(0).id()).isEqualTo(second);
        assertThat(page.content().get(0).verified()).isFalse();
        assertThat(page.content().get(0).beacons()).isZero();
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldSearchUsersByEmailOrNameIgnoringCase() {
        long byEmail = user("zq-needle-1@example.com", "Ann", true, "1 day");
        long byName = user("zq-other@example.com", "Mr NEEDLE", true, "2 days");
        user("zq-hay@example.com", "Bob", true, "3 days");

        PageResponse<AdminUserResponse> page = service.users("Needle", null, 0, 10);

        assertThat(page.content()).extracting(AdminUserResponse::id).containsExactly(byName, byEmail);
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void shouldTreatLikeWildcardsInQueryAsLiterals() {
        long literal = user("zw-50%off@example.com", "Ann", true, "1 day");
        user("zw-50xoff@example.com", "Bob", true, "2 days");
        long underscore = user("zw_a@example.com", "Cid", true, "3 days");
        user("zwxa@example.com", "Dan", true, "4 days");

        assertThat(service.users("50%off", null, 0, 10).content()).extracting(AdminUserResponse::id)
                .containsExactly(literal);
        assertThat(service.users("%", null, 0, 10).content()).extracting(AdminUserResponse::id)
                .containsExactly(literal);
        assertThat(service.users("zw_a", null, 0, 10).content()).extracting(AdminUserResponse::id)
                .containsExactly(underscore);
    }

    @Test
    void shouldFilterUsersByVerification() {
        long verified = user("zv-yes@example.com", "Ann", true, "1 day");
        long unverified = user("zv-no@example.com", "Bob", false, "2 days");

        assertThat(service.users("zv-", false, 0, 10).content()).extracting(AdminUserResponse::id)
                .containsExactly(unverified);
        assertThat(service.users("zv-", true, 0, 10).content()).extracting(AdminUserResponse::id)
                .containsExactly(verified);
        assertThat(service.users("zv-", null, 0, 10).content()).hasSize(2);
    }

    private static Long count(List<SearchQueryStat> stats, String query) {
        return stats.stream().filter(stat -> stat.query().equals(query)).map(SearchQueryStat::searches).findFirst().orElse(null);
    }

    private void event(String level, String message, String ahead) {
        jdbc.update("INSERT INTO system_events (level, source, message, created_at) VALUES (?, 'test', ?, NOW() + ?::interval)",
                level, message, ahead);
    }

    /** {@code ago} is an interval such as "3 days". */
    private void search(String query, int times, int results, String ago) {
        for (int i = 0; i < times; i++) {
            jdbc.update("INSERT INTO search_log (query, query_norm, results_count, created_at) VALUES (?, ?, ?, NOW() - ?::interval)",
                    query, query, results, ago);
        }
    }

    private long user(String email, String name, boolean verified, String ahead) {
        return jdbc.queryForObject("""
                INSERT INTO users (email, password, name, email_verified, created_at)
                VALUES (?, 'x', ?, ?, NOW() + ?::interval) RETURNING id""", Long.class, email, name, verified, ahead);
    }

    private long edition() {
        Long bookId = jdbc.queryForObject(
                "INSERT INTO books (title, title_norm) VALUES ('Journal', 'journal') RETURNING id", Long.class);
        return jdbc.queryForObject("INSERT INTO editions (book_id) VALUES (?) RETURNING id", Long.class, bookId);
    }

    private void beacon(long userId, long editionId) {
        jdbc.update("INSERT INTO beacons (user_id, edition_id, condition_type) VALUES (?, ?, 'ANY_DROP')", userId, editionId);
    }
}
