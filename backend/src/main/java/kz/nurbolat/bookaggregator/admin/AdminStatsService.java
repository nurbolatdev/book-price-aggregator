package kz.nurbolat.bookaggregator.admin;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import kz.nurbolat.bookaggregator.admin.dto.AdminActivityDay;
import kz.nurbolat.bookaggregator.admin.dto.AdminStatsResponse;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    static final String SEARCH_REQUESTS = "kr.search.requests";
    private static final int MAX_ACTIVITY_DAYS = 90;

    private final JdbcTemplate jdbc;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    /** One query per group of figures; the result is cached for a minute per period. */
    @Cacheable(cacheNames = RedisConfig.ADMIN_STATS, key = "#period.code()")
    @Transactional(readOnly = true)
    public AdminStatsResponse stats(StatsPeriod period) {
        Timestamp since = Timestamp.from(clock.instant().minus(period.duration()));
        return jdbc.queryForObject("""
                WITH u AS (
                    SELECT COUNT(*) AS total,
                           COUNT(*) FILTER (WHERE created_at >= ?) AS fresh,
                           COUNT(*) FILTER (WHERE email_verified) AS verified
                    FROM users
                ), active AS (
                    SELECT COUNT(DISTINCT user_id) AS cnt FROM (
                        SELECT user_id FROM search_log WHERE created_at >= ? AND user_id IS NOT NULL
                        UNION
                        SELECT user_id FROM refresh_tokens WHERE created_at >= ?
                    ) t
                ), b AS (
                    SELECT COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active,
                           COUNT(*) FILTER (WHERE last_triggered_at >= ?) AS triggered
                    FROM beacons
                ), n AS (
                    SELECT COUNT(*) FILTER (WHERE status = 'SENT' AND sent_at >= ?) AS sent,
                           COUNT(*) FILTER (WHERE status = 'FAILED' AND created_at >= ?) AS failed
                    FROM notifications
                ), s AS (
                    SELECT COUNT(*) AS cnt FROM search_log WHERE created_at >= ?
                ), c AS (
                    SELECT (SELECT COUNT(*) FROM books) AS books,
                           (SELECT COUNT(*) FROM editions) AS editions,
                           (SELECT COUNT(*) FROM offers WHERE match_status = 'MATCHED') AS matched,
                           (SELECT COUNT(*) FROM offers WHERE match_status = 'PENDING_REVIEW') AS pending
                )
                SELECT u.total, u.fresh, u.verified, active.cnt, b.active, b.triggered, n.sent, n.failed,
                       s.cnt, c.books, c.editions, c.matched, c.pending
                FROM u, active, b, n, s, c
                """,
                (rs, row) -> new AdminStatsResponse(
                        period.code(),
                        new AdminStatsResponse.Users(rs.getLong(1), rs.getLong(2), rs.getLong(3)),
                        rs.getLong(4),
                        new AdminStatsResponse.Beacons(rs.getLong(5), rs.getLong(6)),
                        new AdminStatsResponse.Emails(rs.getLong(7), rs.getLong(8)),
                        rs.getLong(9),
                        cacheHitPercent(),
                        new AdminStatsResponse.Catalog(rs.getLong(10), rs.getLong(11), rs.getLong(12), rs.getLong(13))),
                since, since, since, since, since, since, since);
    }

    /** Days with no events are present with zeros; days are UTC. */
    @Transactional(readOnly = true)
    public List<AdminActivityDay> activity(int days) {
        if (days < 1 || days > MAX_ACTIVITY_DAYS) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "days должен быть от 1 до " + MAX_ACTIVITY_DAYS);
        }
        LocalDate to = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate from = to.minusDays(days - 1L);
        Timestamp startOfDay = Timestamp.from(from.atStartOfDay(ZoneOffset.UTC).toInstant());
        return jdbc.query("""
                SELECT d.day::date,
                       COALESCE(s.cnt, 0), COALESCE(r.cnt, 0), COALESCE(b.cnt, 0), COALESCE(e.cnt, 0)
                FROM generate_series(?::date, ?::date, INTERVAL '1 day') AS d(day)
                LEFT JOIN (SELECT (created_at AT TIME ZONE 'UTC')::date AS day, COUNT(*) AS cnt
                           FROM search_log WHERE created_at >= ? GROUP BY 1) s ON s.day = d.day
                LEFT JOIN (SELECT (created_at AT TIME ZONE 'UTC')::date AS day, COUNT(*) AS cnt
                           FROM users WHERE created_at >= ? GROUP BY 1) r ON r.day = d.day
                LEFT JOIN (SELECT (created_at AT TIME ZONE 'UTC')::date AS day, COUNT(*) AS cnt
                           FROM beacons WHERE created_at >= ? GROUP BY 1) b ON b.day = d.day
                LEFT JOIN (SELECT (sent_at AT TIME ZONE 'UTC')::date AS day, COUNT(*) AS cnt
                           FROM notifications WHERE status = 'SENT' AND sent_at >= ? GROUP BY 1) e ON e.day = d.day
                ORDER BY d.day
                """,
                (rs, row) -> new AdminActivityDay(rs.getObject(1, LocalDate.class),
                        rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)),
                from, to, startOfDay, startOfDay, startOfDay, startOfDay);
    }

    private Double cacheHitPercent() {
        double hits = counted("hit");
        double misses = counted("miss");
        double total = hits + misses;
        return total == 0 ? null : Math.round(hits / total * 1000) / 10.0;
    }

    private double counted(String cacheTag) {
        return Search.in(meterRegistry).name(SEARCH_REQUESTS).tag("cache", cacheTag).counters().stream()
                .mapToDouble(counter -> counter.count()).sum();
    }
}
