package kz.nurbolat.bookaggregator.notification;

/**
 * How a notification reaches the user, fixed when it is created.
 */
public enum NotificationDelivery {
    /** Its own email right away. */
    INSTANT,
    /** One of the books in the next daily digest. */
    DIGEST
}
