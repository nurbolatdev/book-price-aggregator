package kz.nurbolat.bookaggregator.offer;

import jakarta.persistence.*;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.store.Store;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A product in a store. Unique by store and the store's own product id;
 * the edition is set once the offer is matched.
 */
@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edition_id")
    private Edition edition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "title_raw", length = 500)
    private String titleRaw;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "old_price", precision = 10, scale = 2)
    private BigDecimal oldPrice;

    @Column(name = "in_stock", nullable = false)
    private boolean inStock = true;

    @Column(name = "delivery_text")
    private String deliveryText;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 20)
    private MatchStatus matchStatus = MatchStatus.PENDING_REVIEW;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "last_changed_at", nullable = false)
    private Instant lastChangedAt;
}
