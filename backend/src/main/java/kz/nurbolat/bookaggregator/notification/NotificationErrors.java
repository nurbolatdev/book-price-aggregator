package kz.nurbolat.bookaggregator.notification;

import java.util.Set;

/**
 * Reasons in {@code notifications.error}. The final ones close the notification for good: a copy of its email
 * that turns up later is not sent. Any other reason (an SMTP error) leaves it open for the next attempt.
 */
public final class NotificationErrors {

    /** The user's address is not confirmed. */
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    /** The beacon was deleted or paused before its email went out. */
    public static final String BEACON_INACTIVE = "BEACON_INACTIVE";
    /** The broker never confirmed the email within 24 hours. */
    public static final String NOT_PUBLISHED = "NOT_PUBLISHED";

    public static final Set<String> FINAL = Set.of(EMAIL_NOT_VERIFIED, BEACON_INACTIVE, NOT_PUBLISHED);

    private NotificationErrors() {
    }

    public static boolean isFinal(Notification notification) {
        return notification.getStatus() == NotificationStatus.FAILED && FINAL.contains(notification.getError());
    }
}
