package kz.nurbolat.bookaggregator.notification;

import kz.nurbolat.bookaggregator.beacon.Beacon;
import kz.nurbolat.bookaggregator.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;

/**
 * History of beacon emails ({@code notifications}).
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final Clock clock;

    /** Records an email waiting to be sent, right away or in the daily digest. */
    @Transactional
    public Notification queue(User user, Beacon beacon, Map<String, Object> payload, NotificationDelivery delivery) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setBeacon(beacon);
        notification.setPayload(payload);
        notification.setDelivery(delivery);
        return repository.save(notification);
    }

    /** Records an email that will not be sent, with the reason. */
    @Transactional
    public Notification fail(User user, Beacon beacon, Map<String, Object> payload, NotificationDelivery delivery,
                             String error) {
        Notification notification = queue(user, beacon, payload, delivery);
        notification.setStatus(NotificationStatus.FAILED);
        notification.setError(error);
        return notification;
    }

    @Transactional
    public void markSent(Collection<Long> ids) {
        if (!ids.isEmpty()) {
            repository.markSent(ids, clock.instant());
        }
    }

    /** A notification already sent stays sent. */
    @Transactional
    public void markFailed(Collection<Long> ids, String error) {
        if (!ids.isEmpty()) {
            repository.markFailed(ids, error == null ? "unknown error" : error);
        }
    }

    /**
     * Called after the broker confirmed the email message. Runs in its own transaction: it is called after the
     * commit of the transaction that queued the notifications.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(Collection<Long> ids) {
        if (!ids.isEmpty()) {
            repository.markPublished(ids, clock.instant());
        }
    }

    /** Whether the email for these notifications was already handed to the mail server. */
    @Transactional(readOnly = true)
    public boolean allSent(Collection<Long> ids) {
        return !ids.isEmpty() && repository.countNotSent(ids) == 0;
    }
}
