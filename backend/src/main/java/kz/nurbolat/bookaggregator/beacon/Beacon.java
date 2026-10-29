package kz.nurbolat.bookaggregator.beacon;

import jakarta.persistence.*;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.user.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A user's watch on an edition price; one per user and edition.
 */
@Entity
@Table(name = "beacons",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "edition_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Beacon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "edition_id", nullable = false)
    private Edition edition;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 20)
    private BeaconCondition conditionType;

    @Column(name = "target_price", precision = 10, scale = 2)
    private BigDecimal targetPrice;

    @Column(name = "target_percent")
    private Short targetPercent;

    /** Best price when the beacon was created; reference for ANY_DROP and DISCOUNT_PERCENT. */
    @Column(name = "baseline_price", precision = 10, scale = 2)
    private BigDecimal baselinePrice;

    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BeaconStatus status = BeaconStatus.ACTIVE;

    /** When the status last changed: pause, resume or trigger. */
    @Column(name = "status_changed_at", nullable = false)
    private Instant statusChangedAt;

    /** Guards against sending another email for the same price. */
    @Column(name = "last_notified_price", precision = 10, scale = 2)
    private BigDecimal lastNotifiedPrice;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Sets the status; the change time moves only when the status differs (or was never set),
     * so a repeated trigger of a triggered beacon keeps it.
     */
    @PrePersist
    void initStatusChangedAt() {
        if (statusChangedAt == null) {
            statusChangedAt = Instant.now();
        }
    }

    public void changeStatus(BeaconStatus newStatus, Instant now) {
        if (statusChangedAt == null || status != newStatus) {
            statusChangedAt = now;
        }
        status = newStatus;
    }
}
