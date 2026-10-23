package kz.nurbolat.bookaggregator.offer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;

    /**
     * Matched offers of an edition in enabled stores, store loaded; in-stock first, then by price.
     */
    @Transactional(readOnly = true)
    public List<Offer> findMatchedOffers(Long editionId) {
        return offerRepository.findByEditionWithStore(editionId, MatchStatus.MATCHED);
    }

    /**
     * The cheapest in-stock matched offer of an edition in an enabled store, store loaded.
     */
    @Transactional(readOnly = true)
    public Optional<Offer> findBestOffer(Long editionId) {
        return findMatchedOffers(editionId).stream().findFirst().filter(Offer::isInStock);
    }
}
