package kz.nurbolat.bookaggregator.offer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
