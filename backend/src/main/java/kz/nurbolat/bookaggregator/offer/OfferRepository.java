package kz.nurbolat.bookaggregator.offer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Optional<Offer> findByStoreIdAndExternalId(Long storeId, String externalId);

    List<Offer> findByEditionIdOrderByPriceAsc(Long editionId);
}
