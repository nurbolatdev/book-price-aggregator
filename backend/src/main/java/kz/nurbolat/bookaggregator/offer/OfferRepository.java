package kz.nurbolat.bookaggregator.offer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Optional<Offer> findByStoreIdAndExternalId(Long storeId, String externalId);

    @Query("SELECT o FROM Offer o JOIN FETCH o.store LEFT JOIN FETCH o.edition WHERE o.id = :id")
    Optional<Offer> findWithStoreById(Long id);

    @Query(value = """
            SELECT o FROM Offer o
            JOIN FETCH o.store
            WHERE o.matchStatus = :status
            ORDER BY o.firstSeenAt ASC, o.id ASC
            """,
            countQuery = "SELECT COUNT(o) FROM Offer o WHERE o.matchStatus = :status")
    Page<Offer> findByMatchStatusWithStore(MatchStatus status, Pageable pageable);

    @Query("""
            SELECT o FROM Offer o
            JOIN FETCH o.store s
            WHERE o.edition.id = :editionId
              AND o.matchStatus = :status
              AND s.enabled = true
            ORDER BY o.inStock DESC, o.price ASC, o.id ASC
            """)
    List<Offer> findByEditionWithStore(Long editionId, MatchStatus status);

    @Modifying
    @Query("UPDATE Offer o SET o.inStock = false WHERE o.store.id = :storeId AND o.inStock = true AND o.lastSeenAt < :seenBefore")
    int markOutOfStockNotSeenSince(Long storeId, Instant seenBefore);

    @Modifying
    @Query("UPDATE Offer o SET o.inStock = false WHERE o.id IN :ids")
    int markOutOfStock(List<Long> ids);
}
