package kz.nurbolat.bookaggregator.notification;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Users with digest notifications not yet handed to the broker. */
    @Query("""
            SELECT DISTINCT n.user.id FROM Notification n
            WHERE n.status = 'QUEUED' AND n.delivery = 'DIGEST' AND n.publishedAt IS NULL""")
    List<Long> findUsersWaitingForDigest();

    @Query("""
            SELECT n FROM Notification n JOIN FETCH n.user LEFT JOIN FETCH n.beacon
            WHERE n.user.id = :userId AND n.status = 'QUEUED' AND n.delivery = 'DIGEST' AND n.publishedAt IS NULL
            ORDER BY n.createdAt, n.id""")
    List<Notification> findDigestOfUser(Long userId);

    /**
     * Instant notifications whose email never reached the broker, created in [from, to). Rows are locked and
     * rows locked by another relay are skipped.
     */
    @Query(value = """
            SELECT * FROM notifications
            WHERE status = 'QUEUED' AND delivery = 'INSTANT' AND published_at IS NULL
              AND created_at >= :from AND created_at < :to
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    List<Notification> lockUnpublishedInstant(Instant from, Instant to, int limit);

    @Modifying
    @Query("UPDATE Notification n SET n.publishedAt = :at WHERE n.id IN :ids AND n.publishedAt IS NULL")
    int markPublished(Collection<Long> ids, Instant at);

    /** Gives up on instant notifications that did not reach the broker in time; finished ones are not touched. */
    @Modifying
    @Query("""
            UPDATE Notification n SET n.status = 'FAILED', n.error = :error
            WHERE n.status = 'QUEUED' AND n.delivery = 'INSTANT' AND n.publishedAt IS NULL AND n.createdAt < :before""")
    int failUnpublishedInstant(Instant before, String error);

    /**
     * Locks the notifications of an email, in id order (no deadlock between emails sharing rows), waiting for
     * whoever holds them: a copy of the same email, the relay or the digest.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notification n WHERE n.id IN :ids ORDER BY n.id")
    List<Notification> lockForSending(Collection<Long> ids);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'SENT', n.sentAt = :sentAt, n.error = NULL WHERE n.id IN :ids")
    int markSent(Collection<Long> ids, Instant sentAt);

    /** Sent notifications and final failures keep their state. */
    @Modifying
    @Query("""
            UPDATE Notification n SET n.status = 'FAILED', n.error = :error
            WHERE n.id IN :ids AND n.status <> 'SENT'
              AND NOT (n.status = 'FAILED' AND COALESCE(n.error, '') IN :finalErrors)""")
    int markFailed(Collection<Long> ids, String error, Collection<String> finalErrors);
}
