package kz.nurbolat.bookaggregator.offer.dto;

import kz.nurbolat.bookaggregator.offer.MatchStatus;
import kz.nurbolat.bookaggregator.offer.Offer;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A store offer as the administrator sees it when linking offers to editions.
 *
 * @param title     title as the store wrote it
 * @param editionId linked edition, {@code null} unless MATCHED
 */
public record ReviewOfferResponse(
        Long id,
        StoreCode store,
        String externalId,
        String url,
        String title,
        BigDecimal price,
        BigDecimal oldPrice,
        boolean inStock,
        MatchStatus matchStatus,
        Long editionId,
        Instant firstSeenAt,
        Instant lastSeenAt
) {

    /** Expects the store (and the edition, if any) to be loaded. */
    public static ReviewOfferResponse from(Offer offer) {
        return new ReviewOfferResponse(
                offer.getId(),
                offer.getStore().getCode(),
                offer.getExternalId(),
                offer.getUrl(),
                offer.getTitleRaw(),
                offer.getPrice(),
                offer.getOldPrice(),
                offer.isInStock(),
                offer.getMatchStatus(),
                offer.getEdition() == null ? null : offer.getEdition().getId(),
                offer.getFirstSeenAt(),
                offer.getLastSeenAt());
    }
}
