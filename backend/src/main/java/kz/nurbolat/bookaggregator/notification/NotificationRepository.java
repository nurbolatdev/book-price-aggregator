package kz.nurbolat.bookaggregator.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Users with notifications waiting for the daily digest. */
    @Query("""
            SELECT DISTINCT n.user.id FROM Notification n
            WHERE n.status = 'QUEUED' AND n.user.notifyFrequency = 'DAILY' AND n.user.emailVerified = true""")
    List<Long> findUsersWaitingForDigest();

    @Query("""
            SELECT n FROM Notification n JOIN FETCH n.user LEFT JOIN FETCH n.beacon
            WHERE n.user.id = :userId AND n.status = 'QUEUED'
            ORDER BY n.createdAt, n.id""")
    List<Notification> findQueuedOfUser(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'SENT', n.sentAt = :sentAt, n.error = NULL WHERE n.id IN :ids")
    int markSent(Collection<Long> ids, Instant sentAt);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'FAILED', n.error = :error WHERE n.id IN :ids AND n.status <> 'SENT'")
    int markFailed(Collection<Long> ids, String error);
}
