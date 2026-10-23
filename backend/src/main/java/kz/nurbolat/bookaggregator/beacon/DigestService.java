package kz.nurbolat.bookaggregator.beacon;

import kz.nurbolat.bookaggregator.common.text.Emails;
import kz.nurbolat.bookaggregator.config.AppProperties;
import kz.nurbolat.bookaggregator.notification.EmailPublisher;
import kz.nurbolat.bookaggregator.notification.EmailType;
import kz.nurbolat.bookaggregator.notification.Notification;
import kz.nurbolat.bookaggregator.notification.NotificationErrors;
import kz.nurbolat.bookaggregator.notification.NotificationRepository;
import kz.nurbolat.bookaggregator.notification.NotificationService;
import kz.nurbolat.bookaggregator.notification.PriceDropEmail;
import kz.nurbolat.bookaggregator.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily digest: the digest notifications of a user that were not yet handed to the broker go out as one email;
 * a beacon that fired several times appears once, with its latest price. Notifications of beacons deleted or
 * paused meanwhile are not sent. Which notifications are digest ones was fixed when they were created, so a
 * later change of the user's setting does not drop or duplicate them. Each user is handled in their own
 * transaction; the email leaves after its commit and marks the notifications published once the broker confirms
 * it, so a digest already in the queue or in the dead-letter queue is not put together again. A digest that did not
 * reach the broker is retried by the next run; a missed run is caught up the same way.
 */
@Slf4j
@Service
public class DigestService {

    private final NotificationRepository notifications;
    private final NotificationService notificationService;
    private final EmailPublisher emailPublisher;
    private final BeaconAlerts alerts;
    private final AppProperties appProperties;
    private final TransactionTemplate transaction;

    public DigestService(NotificationRepository notifications, NotificationService notificationService,
                         EmailPublisher emailPublisher, BeaconAlerts alerts, AppProperties appProperties,
                         PlatformTransactionManager transactionManager) {
        this.notifications = notifications;
        this.notificationService = notificationService;
        this.emailPublisher = emailPublisher;
        this.alerts = alerts;
        this.appProperties = appProperties;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * @return how many digest emails were queued
     */
    public int sendDailyDigests() {
        int sent = 0;
        for (Long userId : notifications.findUsersWaitingForDigest()) {
            Boolean queued = transaction.execute(status -> sendTo(userId));
            if (Boolean.TRUE.equals(queued)) {
                sent++;
            }
        }
        log.info("Daily digest: {} emails queued", sent);
        return sent;
    }

    private boolean sendTo(Long userId) {
        List<Notification> queued = notifications.findDigestOfUser(userId);
        if (queued.isEmpty()) {
            return false;
        }
        User user = queued.get(0).getUser();
        // The latest notification of each beacon wins; earlier ones go out with it
        Map<Long, Notification> latest = new LinkedHashMap<>();
        List<Long> delivered = new ArrayList<>();
        List<Long> inactive = new ArrayList<>();
        for (Notification notification : queued) {
            Beacon beacon = notification.getBeacon();
            if (beacon == null || beacon.getStatus() == BeaconStatus.PAUSED) {
                inactive.add(notification.getId());
                continue;
            }
            latest.remove(beacon.getId());
            latest.put(beacon.getId(), notification);
            delivered.add(notification.getId());
        }
        notificationService.markFailed(inactive, NotificationErrors.BEACON_INACTIVE);
        if (latest.isEmpty()) {
            return false;
        }

        String baseUrl = appProperties.baseUrl();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Notification notification : latest.values()) {
            Map<String, Object> item = PriceDropEmail.item(notification.getPayload(), baseUrl);
            item.put("unsubscribeUrl", alerts.unsubscribeUrl(notification.getBeacon()));
            items.add(item);
        }
        Map<String, Object> model = new HashMap<>();
        model.put("name", user.getName());
        model.put("items", items);
        model.put("beaconsUrl", baseUrl + "/beacons");
        model.put("settingsUrl", baseUrl + "/profile");
        emailPublisher.publish(EmailType.DIGEST, user.getEmail(), subject(items.size()), model, delivered);
        log.info("Daily digest for {}: {} books", Emails.mask(user.getEmail()), items.size());
        return true;
    }

    /** {@code Сводка цен: подешевели 3 книги} */
    static String subject(int books) {
        int lastTwo = books % 100;
        int last = books % 10;
        String word = lastTwo >= 11 && lastTwo <= 14 ? "книг"
                : last == 1 ? "книга"
                : last >= 2 && last <= 4 ? "книги"
                : "книг";
        String verb = last == 1 && lastTwo != 11 ? "подешевела" : "подешевели";
        return "Сводка цен: " + verb + " " + books + " " + word + " — KitapRadar";
    }
}
