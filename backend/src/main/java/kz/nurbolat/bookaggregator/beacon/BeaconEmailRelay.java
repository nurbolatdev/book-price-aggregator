package kz.nurbolat.bookaggregator.beacon;

import kz.nurbolat.bookaggregator.notification.EmailPublisher;
import kz.nurbolat.bookaggregator.notification.EmailType;
import kz.nurbolat.bookaggregator.notification.Notification;
import kz.nurbolat.bookaggregator.notification.NotificationErrors;
import kz.nurbolat.bookaggregator.notification.NotificationRepository;
import kz.nurbolat.bookaggregator.notification.NotificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Publishes again the instant beacon emails that never reached RabbitMQ: the notification was committed, but the
 * broker was down or the application stopped before it confirmed the message ({@code published_at} is empty).
 * <ul>
 *     <li>Only {@code QUEUED} + {@code INSTANT} + unpublished rows: emails in a queue or in the dead-letter queue
 *     are published, so their retries stay with RabbitMQ; digest and refused notifications are not touched.</li>
 *     <li>Only rows older than {@link #MIN_AGE}: the first publication gives up after
 *     {@link EmailPublisher#CONFIRM_TIMEOUT}, so it is over by then and both never publish the same row at once.</li>
 *     <li>Rows are locked with {@code SKIP LOCKED}; together with ShedLock no two relays take the same row.</li>
 *     <li>After {@link #GIVE_UP} unpublished rows become {@code FAILED} ({@code NOT_PUBLISHED}), a final reason:
 *     if a copy did reach the broker after all (its confirmation was lost) and turns up later, the listener
 *     acknowledges it without sending.</li>
 * </ul>
 * If the broker took a message but its confirmation was lost, the email is published twice; the listener sends
 * it once, because it checks that the notification is not {@code SENT} yet.
 */
@Slf4j
@Service
public class BeaconEmailRelay {

    static final Duration MIN_AGE = Duration.ofMinutes(1);
    static final Duration GIVE_UP = Duration.ofHours(24);
    private static final int BATCH = 100;

    private final NotificationRepository notifications;
    private final BeaconAlerts alerts;
    private final EmailPublisher emailPublisher;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public BeaconEmailRelay(NotificationRepository notifications, BeaconAlerts alerts, EmailPublisher emailPublisher,
                            PlatformTransactionManager transactionManager, Clock clock) {
        this.notifications = notifications;
        this.alerts = alerts;
        this.emailPublisher = emailPublisher;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public Result run() {
        Instant now = clock.instant();
        Integer republished = transaction.execute(status -> republish(now));
        Integer abandoned = transaction.execute(status -> notifications.failUnpublishedInstant(now.minus(GIVE_UP), NotificationErrors.NOT_PUBLISHED));
        Result result = new Result(republished == null ? 0 : republished, abandoned == null ? 0 : abandoned);
        if (result.republished() > 0 || result.abandoned() > 0) {
            log.warn("Beacon email relay: {} emails published again, {} given up", result.republished(), result.abandoned());
        }
        return result;
    }

    private int republish(Instant now) {
        List<Notification> stuck = notifications.lockUnpublishedInstant(now.minus(GIVE_UP), now.minus(MIN_AGE), BATCH);
        int published = 0;
        for (Notification notification : stuck) {
            Beacon beacon = notification.getBeacon();
            if (beacon == null || beacon.getStatus() == BeaconStatus.PAUSED) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setError(NotificationErrors.BEACON_INACTIVE);
                continue;
            }
            BeaconAlerts.Email email = alerts.instantEmail(notification);
            if (emailPublisher.publishNow(EmailType.PRICE_DROP, notification.getUser().getEmail(), email.subject(),
                    email.model(), List.of(notification.getId()))) {
                notification.setPublishedAt(clock.instant());
                published++;
            }
        }
        return published;
    }

    public record Result(int republished, int abandoned) {
    }
}
