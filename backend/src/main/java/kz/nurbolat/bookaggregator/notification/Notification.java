package kz.nurbolat.bookaggregator.notification;

import jakarta.persistence.*;
import kz.nurbolat.bookaggregator.beacon.Beacon;
import kz.nurbolat.bookaggregator.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * One beacon email to one user: its content ({@code payload}) and what happened to it.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beacon_id")
    private Beacon beacon;

    @Column(nullable = false, length = 20)
    private String channel = "EMAIL";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationDelivery delivery = NotificationDelivery.INSTANT;

    /** When RabbitMQ confirmed the email message; {@code null} — never reached the broker. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> payload;

    private String error;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** When the notification was queued, or queued again by an admin; the relay window counts from it. */
    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;
}
