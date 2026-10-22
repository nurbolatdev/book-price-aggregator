package kz.nurbolat.bookaggregator.offer;

import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import kz.nurbolat.bookaggregator.parser.matching.EditionMatcher;
import kz.nurbolat.bookaggregator.parser.matching.MatchResult;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The only place where store offers are written (docs/05-parsing-policy.md): upsert by store and
 * product id, edition matching for offers not linked yet, a price_history row only when the price
 * appears or changes, and a {@link PriceChangedEvent} for matched offers, delivered after commit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferIngestionService {

    private static final int TITLE_LENGTH = 500;
    private static final int DELIVERY_LENGTH = 255;

    private final OfferRepository offerRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final StoreService storeService;
    private final EditionMatcher editionMatcher;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * Stores one batch in one transaction. The same product listed twice keeps the last values.
     *
     * @param runId parser run that produced the batch, for logs
     */
    @Transactional
    public IngestResult ingest(StoreCode storeCode, List<ParsedOffer> items, Long runId) {
        Store store = storeService.getByCode(storeCode);
        Instant now = clock.instant();
        Map<String, ParsedOffer> unique = new LinkedHashMap<>();
        items.forEach(item -> unique.put(item.externalId(), item));

        int upserted = 0;
        int priceChanged = 0;
        int matched = 0;
        int pendingReview = 0;
        for (ParsedOffer item : unique.values()) {
            if (item.price() == null || item.externalId() == null || item.url() == null) {
                log.warn("Skipped incomplete offer from {} (run {}): {}", storeCode, runId, item);
                continue;
            }
            Offer offer = offerRepository.findByStoreIdAndExternalId(store.getId(), item.externalId())
                    .orElseGet(() -> newOffer(store, item.externalId(), now));
            BigDecimal previousPrice = offer.getId() == null ? null : offer.getPrice();
            boolean changed = previousPrice == null || previousPrice.compareTo(item.price()) != 0;

            apply(offer, item, now);
            if (changed) {
                offer.setLastChangedAt(now);
            }
            if (offer.getEdition() == null && offer.getMatchStatus() != MatchStatus.REJECTED) {
                MatchResult match = editionMatcher.match(item);
                offer.setMatchStatus(match.status());
                offer.setEdition(match.edition());
                if (match.status() == MatchStatus.MATCHED) {
                    matched++;
                } else if (match.status() == MatchStatus.PENDING_REVIEW) {
                    pendingReview++;
                }
            }
            offerRepository.save(offer);
            upserted++;

            if (changed) {
                recordHistory(offer, item, now);
                priceChanged++;
                if (offer.getEdition() != null) {
                    events.publishEvent(new PriceChangedEvent(offer.getId(), offer.getEdition().getId(), storeCode,
                            previousPrice, item.price(), now));
                }
            }
        }
        log.debug("Ingested {} offers from {} (run {}), {} price changes", upserted, storeCode, runId, priceChanged);
        return new IngestResult(upserted, priceChanged, matched, pendingReview);
    }

    /**
     * Offers of a store not seen for the given time are considered gone and marked out of stock.
     *
     * @return number of offers marked
     */
    @Transactional
    public int markUnseenOutOfStock(StoreCode storeCode, Duration notSeenFor) {
        Store store = storeService.getByCode(storeCode);
        return offerRepository.markOutOfStockNotSeenSince(store.getId(), clock.instant().minus(notSeenFor));
    }

    /**
     * Offers whose product pages disappeared from the store.
     */
    @Transactional
    public int markOutOfStock(List<Long> offerIds) {
        return offerIds.isEmpty() ? 0 : offerRepository.markOutOfStock(offerIds);
    }

    private static Offer newOffer(Store store, String externalId, Instant now) {
        Offer offer = new Offer();
        offer.setStore(store);
        offer.setExternalId(externalId);
        offer.setFirstSeenAt(now);
        return offer;
    }

    private static void apply(Offer offer, ParsedOffer item, Instant now) {
        offer.setUrl(item.url());
        offer.setTitleRaw(truncate(item.title(), TITLE_LENGTH));
        offer.setPrice(item.price());
        offer.setOldPrice(item.oldPrice());
        offer.setInStock(item.inStock());
        offer.setDeliveryText(truncate(item.deliveryText(), DELIVERY_LENGTH));
        offer.setLastSeenAt(now);
    }

    private void recordHistory(Offer offer, ParsedOffer item, Instant now) {
        PriceHistory history = new PriceHistory();
        history.setOffer(offer);
        history.setPrice(item.price());
        history.setOldPrice(item.oldPrice());
        history.setRecordedAt(now);
        priceHistoryRepository.save(history);
    }

    private static String truncate(String value, int length) {
        return value == null || value.length() <= length ? value : value.substring(0, length);
    }
}
