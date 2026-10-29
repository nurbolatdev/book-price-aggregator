package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.admin.dto.AdminEventResponse;
import kz.nurbolat.bookaggregator.admin.dto.AdminUserResponse;
import kz.nurbolat.bookaggregator.admin.dto.SearchQueryStat;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/** Read-only admin lists: event journal, search analytics, users. */
@Service
@RequiredArgsConstructor
public class AdminJournalService {

    static final int TOP_LIMIT = 20;

    private final SystemEventRepository eventRepository;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    /** Newest first; {@code level} null for every level. */
    @Transactional(readOnly = true)
    public PageResponse<AdminEventResponse> events(SystemEvent.Level level, int page, int size) {
        PageRequest request = PageRequest.of(page, size);
        Page<SystemEvent> events = level == null
                ? eventRepository.findAllByOrderByCreatedAtDescIdDesc(request)
                : eventRepository.findByLevelOrderByCreatedAtDescIdDesc(level, request);
        return PageResponse.of(events.map(AdminEventResponse::from).getContent(), page, size, events.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<SearchQueryStat> topQueries(StatsPeriod period) {
        return jdbc.query("""
                        SELECT query_norm, COUNT(*) AS cnt FROM search_log
                        WHERE created_at >= ? AND query_norm IS NOT NULL
                        GROUP BY query_norm ORDER BY cnt DESC, query_norm LIMIT ?
                        """,
                (rs, row) -> new SearchQueryStat(rs.getString(1), rs.getLong(2)), since(period), TOP_LIMIT);
    }

    /** Queries that found nothing, the most frequent first. */
    @Transactional(readOnly = true)
    public List<SearchQueryStat> zeroResultQueries(StatsPeriod period) {
        return jdbc.query("""
                        SELECT query_norm, COUNT(*) AS cnt FROM search_log
                        WHERE created_at >= ? AND query_norm IS NOT NULL AND results_count = 0
                        GROUP BY query_norm ORDER BY cnt DESC, query_norm LIMIT ?
                        """,
                (rs, row) -> new SearchQueryStat(rs.getString(1), rs.getLong(2)), since(period), TOP_LIMIT);
    }

    /**
     * Newest first; the email is shown in full, this is the admin panel. {@code q} matches a part of the email or
     * the name, ignoring case; {@code verified} null — all users.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(String q, Boolean verified, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE TRUE");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String pattern = "%" + escapeLike(q.strip()) + "%";
            where.append(" AND (u.email ILIKE ? ESCAPE '\\' OR u.name ILIKE ? ESCAPE '\\')");
            args.add(pattern);
            args.add(pattern);
        }
        if (verified != null) {
            where.append(" AND u.email_verified = ?");
            args.add(verified);
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM users u" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<AdminUserResponse> users = jdbc.query("""
                        SELECT u.id, u.email, u.name, u.created_at, u.email_verified,
                               (SELECT COUNT(*) FROM beacons b WHERE b.user_id = u.id)
                        FROM users u""" + where + " ORDER BY u.created_at DESC, u.id DESC LIMIT ? OFFSET ?",
                (rs, row) -> new AdminUserResponse(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getTimestamp(4).toInstant(), rs.getBoolean(5), rs.getLong(6)),
                pageArgs.toArray());
        return PageResponse.of(users, page, size, total == null ? 0 : total);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Timestamp since(StatsPeriod period) {
        return Timestamp.from(clock.instant().minus(period.duration()));
    }
}
