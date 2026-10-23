package kz.nurbolat.bookaggregator.notification;

import kz.nurbolat.bookaggregator.beacon.Beacon;
import kz.nurbolat.bookaggregator.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

    /** Records an email waiting to be sent, now or in the daily digest. */
    @Transactional
    public Notification queue(User user, Beacon beacon, Map<String, Object> payload) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setBeacon(beacon);
        notification.setPayload(payload);
        return repository.save(notification);
    }

    /** Records an email that will not be sent, with the reason. */
    @Transactional
    public Notification fail(User user, Beacon beacon, Map<String, Object> payload, String error) {
        Notification notification = queue(user, beacon, payload);
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
}
