package kz.nurbolat.bookaggregator.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'SENT', n.sentAt = :sentAt, n.error = NULL WHERE n.id IN :ids")
    int markSent(Collection<Long> ids, Instant sentAt);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'FAILED', n.error = :error WHERE n.id IN :ids AND n.status <> 'SENT'")
    int markFailed(Collection<Long> ids, String error);
}
