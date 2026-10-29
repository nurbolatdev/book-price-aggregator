package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.admin.dto.AdminNotificationResponse;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.notification.Notification;
import kz.nurbolat.bookaggregator.notification.NotificationDelivery;
import kz.nurbolat.bookaggregator.notification.NotificationRepository;
import kz.nurbolat.bookaggregator.notification.NotificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Admin view of the notification log and manual retry of failed emails. */
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    static final String DIGEST_TITLE = "Сводка за день";
    static final String WEEKLY_TITLE = "Подборка недели";

    private final JdbcTemplate jdbc;
    private final NotificationRepository repository;
    private final SystemEventService events;
    private final Clock clock;

    /** Newest first; {@code status} and {@code delivery} are optional filters. */
    @Transactional(readOnly = true)
    public PageResponse<AdminNotificationResponse> list(NotificationStatus status, NotificationDelivery delivery,
                                                        int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE TRUE");
        List<Object> args = new ArrayList<>();
        if (status != null) {
            where.append(" AND n.status = ?");
            args.add(status.name());
        }
        if (delivery != null) {
            where.append(" AND n.delivery = ?");
            args.add(delivery.name());
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM notifications n" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<AdminNotificationResponse> content = jdbc.query("""
                        SELECT n.id, n.created_at, n.sent_at, u.email,
                               COALESCE(n.payload ->> 'title',
                                        CASE n.delivery WHEN 'DIGEST' THEN ? WHEN 'WEEKLY' THEN ? END),
                               n.delivery, n.status, n.error
                        FROM notifications n JOIN users u ON u.id = n.user_id
                        """ + where + " ORDER BY n.created_at DESC, n.id DESC LIMIT ? OFFSET ?",
                (rs, row) -> new AdminNotificationResponse(rs.getLong(1), rs.getTimestamp(2).toInstant(),
                        instant(rs.getTimestamp(3)), rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7), rs.getString(8)),
                prepend(pageArgs, DIGEST_TITLE, WEEKLY_TITLE));
        return PageResponse.of(content, page, size, total == null ? 0 : total);
    }

    /**
     * Puts a {@code FAILED} notification back in the queue. The relay publishes instant and weekly emails
     * a minute later, the daily digest takes a digest notification with its next run.
     */
    @Transactional
    public AdminNotificationResponse retry(Long id) {
        Notification notification = repository.lockWithUser(id)
                .orElseThrow(() -> new NotFoundException("Уведомление не найдено"));
        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new AppException(ErrorCode.CONFLICT, "Повторить можно только неудачное уведомление");
        }
        if (!notification.getUser().isEmailVerified()) {
            throw new AppException(ErrorCode.CONFLICT, "Email пользователя не подтверждён");
        }
        notification.setStatus(NotificationStatus.QUEUED);
        notification.setPublishedAt(null);
        notification.setError(null);
        notification.setQueuedAt(clock.instant());
        events.info("admin", "Повтор отправки уведомления", Map.of("notificationId", notification.getId()));
        return new AdminNotificationResponse(notification.getId(), notification.getCreatedAt(), notification.getSentAt(),
                notification.getUser().getEmail(), title(notification), notification.getDelivery().name(),
                notification.getStatus().name(), notification.getError());
    }

    private static String title(Notification notification) {
        Object title = notification.getPayload() == null ? null : notification.getPayload().get("title");
        if (title != null) {
            return title.toString();
        }
        return switch (notification.getDelivery()) {
            case DIGEST -> DIGEST_TITLE;
            case WEEKLY -> WEEKLY_TITLE;
            default -> null;
        };
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Object[] prepend(List<Object> args, Object first, Object second) {
        List<Object> all = new ArrayList<>();
        all.add(first);
        all.add(second);
        all.addAll(args);
        return all.toArray();
    }
}
