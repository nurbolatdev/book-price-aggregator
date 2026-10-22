package kz.nurbolat.bookaggregator.offer;

import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.edition.EditionRepository;
import kz.nurbolat.bookaggregator.offer.dto.ReviewOfferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Manual review of offers the matcher could not link with confidence (docs/05-parsing-policy.md, section 3):
 * the administrator links an offer to an edition or rejects it as not a book.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferReviewService {

    private final OfferRepository offerRepository;
    private final EditionRepository editionRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * Offers waiting for review, the longest waiting first.
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewOfferResponse> pendingReview(int page, int size) {
        Page<Offer> offers = offerRepository.findByMatchStatusWithStore(MatchStatus.PENDING_REVIEW,
                PageRequest.of(page, size));
        return PageResponse.of(offers.map(ReviewOfferResponse::from).getContent(), page, size,
                offers.getTotalElements());
    }

    /**
     * Links the offer to the edition, also when it was matched or rejected before. From now on its price
     * counts for the edition, so a {@link PriceChangedEvent} without an old price is published, as for a new offer.
     */
    @Transactional
    public ReviewOfferResponse match(Long offerId, Long editionId) {
        Offer offer = find(offerId);
        Edition edition = editionRepository.findById(editionId)
                .orElseThrow(() -> new NotFoundException("Edition not found: " + editionId));
        offer.setEdition(edition);
        offer.setMatchStatus(MatchStatus.MATCHED);
        events.publishEvent(new PriceChangedEvent(offer.getId(), edition.getId(), offer.getStore().getCode(),
                null, offer.getPrice(), clock.instant()));
        log.info("Offer {} of {} matched to edition {} manually", offerId, offer.getStore().getCode(), editionId);
        return ReviewOfferResponse.from(offer);
    }

    /**
     * Marks the offer as not a book; ingestion never tries to match it again.
     */
    @Transactional
    public ReviewOfferResponse reject(Long offerId) {
        Offer offer = find(offerId);
        offer.setEdition(null);
        offer.setMatchStatus(MatchStatus.REJECTED);
        log.info("Offer {} of {} rejected manually", offerId, offer.getStore().getCode());
        return ReviewOfferResponse.from(offer);
    }

    private Offer find(Long offerId) {
        return offerRepository.findWithStoreById(offerId)
                .orElseThrow(() -> new NotFoundException("Offer not found: " + offerId));
    }
}
