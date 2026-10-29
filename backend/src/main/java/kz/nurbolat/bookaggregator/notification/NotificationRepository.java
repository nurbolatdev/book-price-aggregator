package kz.nurbolat.bookaggregator.notification;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    /** Whether the user already has the weekly digest of this ISO week ({@code 2026-W44}). */
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM notifications
                           WHERE user_id = :userId AND delivery = 'WEEKLY' AND payload ->> 'isoWeek' = :isoWeek)""",
            nativeQuery = true)
    boolean existsWeekly(Long userId, String isoWeek);

    /**
     * Instant and weekly notifications whose email never reached the broker, queued in [from, to). Rows are
     * locked and rows locked by another relay are skipped.
     */
    @Query(value = """
            SELECT * FROM notifications
            WHERE status = 'QUEUED' AND delivery IN ('INSTANT', 'WEEKLY') AND published_at IS NULL
              AND queued_at >= :from AND queued_at < :to
            ORDER BY queued_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    List<Notification> lockUnpublished(Instant from, Instant to, int limit);

    /** One notification with its user, locked against a concurrent retry or the relay. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notification n JOIN FETCH n.user WHERE n.id = :id")
    Optional<Notification> lockWithUser(Long id);

    @Modifying
    @Query("UPDATE Notification n SET n.publishedAt = :at WHERE n.id IN :ids AND n.publishedAt IS NULL")
    int markPublished(Collection<Long> ids, Instant at);

    /** Gives up on instant and weekly notifications that did not reach the broker in time; finished ones are not touched. */
    @Modifying
    @Query("""
            UPDATE Notification n SET n.status = 'FAILED', n.error = :error
            WHERE n.status = 'QUEUED' AND n.delivery IN ('INSTANT', 'WEEKLY') AND n.publishedAt IS NULL AND n.queuedAt < :before""")
    int failUnpublished(Instant before, String error);

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
