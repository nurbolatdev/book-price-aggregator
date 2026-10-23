package kz.nurbolat.bookaggregator.notification;

public enum NotificationStatus {
    /** Waiting to be sent: right away, or in the daily digest. */
    QUEUED,
    SENT,
    FAILED
}
