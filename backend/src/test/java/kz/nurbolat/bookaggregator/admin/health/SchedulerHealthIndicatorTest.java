package kz.nurbolat.bookaggregator.admin.health;

import kz.nurbolat.bookaggregator.scheduler.SchedulerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The crawl runs every 6 hours, so a run is missed once the last one is older than 12 hours. */
class SchedulerHealthIndicatorTest {

    private static final Instant NOW = Instant.parse("2026-03-10T10:00:00Z");

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final SchedulerHealthIndicator indicator = new SchedulerHealthIndicator(jdbc,
            new SchedulerProperties("0 0 */6 * * *", "0 30 3 * * *", "0 0 9 * * *", Duration.ofDays(7),
                    Duration.ofDays(90), Duration.ofHours(6), Duration.ofMinutes(30)),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldBeUpWhenLastRunIsRecent() {
        lastRunAt(NOW.minus(Duration.ofHours(5)));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("lastRun", NOW.minus(Duration.ofHours(5)));
        assertThat(health.getDetails().get("nextRun")).isNotNull();
    }

    @Test
    void shouldBeDownWhenLastRunIsOlderThanTwoPeriods() {
        lastRunAt(NOW.minus(Duration.ofHours(13)));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void shouldBeUpWhenLastRunIsWithinTwoPeriods() {
        lastRunAt(NOW.minus(Duration.ofHours(11)));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void shouldBeUpWhenThereIsNoShedlockRow() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(SchedulerHealthIndicator.CRAWL_LOCK))).thenReturn(List.of());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).doesNotContainKey("lastRun");
    }

    @SuppressWarnings("unchecked")
    private void lastRunAt(Instant lastRun) {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(SchedulerHealthIndicator.CRAWL_LOCK)))
                .thenReturn(List.of(lastRun));
    }
}
