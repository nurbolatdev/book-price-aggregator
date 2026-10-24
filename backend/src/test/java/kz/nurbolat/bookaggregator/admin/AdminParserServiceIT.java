package kz.nurbolat.bookaggregator.admin;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import kz.nurbolat.bookaggregator.admin.dto.AdminParserResponse;
import kz.nurbolat.bookaggregator.parser.core.RunStatus;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class AdminParserServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AdminParserService service;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @Autowired
    private StoreService storeService;

    @BeforeEach
    void clearRuns() {
        jdbc.update("DELETE FROM parser_runs");
        storeService.evictCache();
    }

    /** The store list is cached in memory and survives the rollback of the test transaction. */
    @AfterEach
    void evictStoreCache() {
        storeService.evictCache();
    }

    @Test
    void shouldTakeLastRunSuccessShareAndAverageFromParserRuns() {
        Instant now = Instant.now();
        run("FLIP", "SUCCESS", 10, 0, now.minus(Duration.ofDays(3)), Duration.ofSeconds(100));
        run("FLIP", "PARTIAL", 90, 10, now.minus(Duration.ofHours(2)), Duration.ofSeconds(300));
        run("FLIP", "RUNNING", 0, 0, now.minus(Duration.ofMinutes(10)), null);
        run("FLIP", "SUCCESS", 5, 0, now.minus(Duration.ofDays(10)), Duration.ofSeconds(5000));

        AdminParserResponse flip = parser(StoreCode.FLIP);

        assertThat(flip.lastRun().status()).isEqualTo(RunStatus.RUNNING);
        assertThat(flip.lastRun().durationSeconds()).isNull();
        assertThat(flip.lastRun().startedAt()).isCloseTo(now.minus(Duration.ofMinutes(10)),
                org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.SECONDS));
        // 24 hours: only the PARTIAL run counts (90 of 100 pages)
        assertThat(flip.successPercent24h()).isEqualTo(90.0);
        // 7 days: finished runs only, the 10 day old one is outside: (100 + 300) / 2
        assertThat(flip.avgRunSeconds()).isEqualTo(200.0);
    }

    @Test
    void shouldReturnLastFinishedRunWithItsDuration() {
        run("MELOMAN", "FAILED", 0, 4, Instant.now().minus(Duration.ofHours(1)), Duration.ofSeconds(42));

        AdminParserResponse meloman = parser(StoreCode.MELOMAN);

        assertThat(meloman.lastRun().status()).isEqualTo(RunStatus.FAILED);
        assertThat(meloman.lastRun().durationSeconds()).isEqualTo(42L);
        assertThat(meloman.successPercent24h()).isEqualTo(0.0);
    }

    @Test
    void shouldShowEmptyFiguresForStoreWithoutRuns() {
        AdminParserResponse ozon = parser(StoreCode.OZON);

        assertThat(ozon.lastRun()).isNull();
        assertThat(ozon.successPercent24h()).isNull();
        assertThat(ozon.avgRunSeconds()).isNull();
    }

    @Test
    void shouldCountOnlyMatchedOffers() {
        long before = parser(StoreCode.FLIP).matchedOffers();
        long edition = edition();
        offer(edition, "adm-par-1", "MATCHED");
        offer(edition, "adm-par-2", "MATCHED");
        offer(edition, "adm-par-3", "PENDING_REVIEW");
        offer(edition, "adm-par-4", "REJECTED");

        assertThat(parser(StoreCode.FLIP).matchedOffers() - before).isEqualTo(2);
    }

    @Test
    void shouldReportCircuitBreakerState() {
        CircuitBreaker breaker = circuitBreakers.circuitBreaker("store-OZON");
        try {
            assertThat(parser(StoreCode.OZON).circuitBreaker()).isEqualTo("CLOSED");

            breaker.transitionToOpenState();

            assertThat(parser(StoreCode.OZON).circuitBreaker()).isEqualTo("OPEN");
            assertThat(parser(StoreCode.FLIP).circuitBreaker()).isEqualTo("CLOSED");
        } finally {
            breaker.reset();
        }
    }

    @Test
    void shouldSwitchStoreOffAndOnAndNoteItInTheJournal() {
        long eventsBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_events WHERE source = 'store' AND message LIKE '%FLIP%'", Long.class);
        boolean initial = parser(StoreCode.FLIP).enabled();

        AdminParserResponse.StoreState off = service.setEnabled(StoreCode.FLIP, !initial);

        assertThat(off.enabled()).isEqualTo(!initial);
        // The test transaction never commits, so the after-commit eviction does not run here
        storeService.evictCache();
        assertThat(parser(StoreCode.FLIP).enabled()).isEqualTo(!initial);
        assertThat(jdbc.queryForObject("SELECT enabled FROM stores WHERE code = 'FLIP'", Boolean.class)).isEqualTo(!initial);

        // Same value again changes nothing and writes no second event
        service.setEnabled(StoreCode.FLIP, !initial);
        long eventsAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_events WHERE source = 'store' AND message LIKE '%FLIP%'", Long.class);
        // The event is committed in its own transaction, so it stays after the test: remove it
        assertThat(eventsAfter - eventsBefore).isEqualTo(1);
        jdbc.update("DELETE FROM system_events WHERE source = 'store' AND message LIKE '%FLIP%'");
    }

    private AdminParserResponse parser(StoreCode code) {
        List<AdminParserResponse> parsers = service.parsers();
        return parsers.stream().filter(parser -> parser.code() == code).findFirst().orElseThrow();
    }

    private void run(String store, String status, int ok, int failed, Instant startedAt, Duration duration) {
        jdbc.update("""
                INSERT INTO parser_runs (store_id, trigger_type, status, pages_ok, pages_failed, started_at, finished_at)
                SELECT id, 'SCHEDULED', ?, ?, ?, ?, ? FROM stores WHERE code = ?""",
                status, ok, failed, Timestamp.from(startedAt),
                duration == null ? null : Timestamp.from(startedAt.plus(duration)), store);
    }

    private long edition() {
        Long bookId = jdbc.queryForObject(
                "INSERT INTO books (title, title_norm) VALUES ('Parser admin', 'parser admin') RETURNING id", Long.class);
        return jdbc.queryForObject("INSERT INTO editions (book_id) VALUES (?) RETURNING id", Long.class, bookId);
    }

    private void offer(long editionId, String externalId, String status) {
        jdbc.update("""
                INSERT INTO offers (edition_id, store_id, external_id, url, price, match_status,
                                    first_seen_at, last_seen_at, last_changed_at)
                SELECT ?, id, ?, 'https://www.flip.kz/catalog?prod=' || ?, 1000, ?, NOW(), NOW(), NOW()
                FROM stores WHERE code = 'FLIP'""", editionId, externalId, externalId, status);
    }
}
