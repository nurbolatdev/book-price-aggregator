package kz.nurbolat.bookaggregator.offer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Optional<Offer> findByStoreIdAndExternalId(Long storeId, String externalId);

    @Query("""
            SELECT o FROM Offer o
            JOIN FETCH o.store s
            WHERE o.edition.id = :editionId
              AND o.matchStatus = :status
              AND s.enabled = true
            ORDER BY o.inStock DESC, o.price ASC, o.id ASC
            """)
    List<Offer> findByEditionWithStore(Long editionId, MatchStatus status);
}
