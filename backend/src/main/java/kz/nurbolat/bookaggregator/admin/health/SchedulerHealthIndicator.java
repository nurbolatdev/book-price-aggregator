package kz.nurbolat.bookaggregator.admin.health;

import kz.nurbolat.bookaggregator.scheduler.SchedulerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * The crawl scheduler: time of the last run (ShedLock records when a job took its lock) and of the
 * next one by the cron expression. DOWN when the last run is older than two periods, i.e. a run was missed.
 */
@Component("scheduler")
@RequiredArgsConstructor
public class SchedulerHealthIndicator extends AbstractHealthIndicator {

    static final String CRAWL_LOCK = "crawl-stores";

    private final JdbcTemplate jdbc;
    private final SchedulerProperties properties;
    private final Clock clock;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(ZoneId.of(SchedulerProperties.ZONE)));
        CronExpression cron = CronExpression.parse(properties.crawlCron());
        ZonedDateTime next = cron.next(now);
        ZonedDateTime afterNext = next == null ? null : cron.next(next);
        if (next != null) {
            builder.withDetail("nextRun", next.toInstant());
        }

        Instant lastRun = lastRun();
        if (lastRun != null) {
            builder.withDetail("lastRun", lastRun);
        }
        if (lastRun == null || next == null || afterNext == null) {
            // Not run yet (fresh install) is not a failure
            builder.up();
            return;
        }
        long periodSeconds = afterNext.toEpochSecond() - next.toEpochSecond();
        boolean missed = now.toInstant().minusSeconds(2 * periodSeconds).isAfter(lastRun);
        builder.status(missed ? "DOWN" : "UP");
    }

    public Instant lastRun() {
        try {
            List<Instant> runs = jdbc.query("SELECT locked_at FROM shedlock WHERE name = ?",
                    (rs, row) -> rs.getObject(1, Timestamp.class).toInstant(), CRAWL_LOCK);
            return runs.isEmpty() ? null : runs.get(0);
        } catch (DataAccessException e) {
            return null;
        }
    }
}
