package kz.nurbolat.bookaggregator.beacon;

import kz.nurbolat.bookaggregator.messaging.ProcessedMessages;
import kz.nurbolat.bookaggregator.messaging.RabbitTopology;
import kz.nurbolat.bookaggregator.messaging.message.PriceChangedMessage;
import kz.nurbolat.bookaggregator.offer.Offer;
import kz.nurbolat.bookaggregator.offer.OfferService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Set;

/**
 * Checks the beacons of an edition when its price changes ({@code kr.price-changed.beacons}).
 * <ol>
 *     <li>best = the cheapest in-stock matched offer; no such offer — nothing to check;</li>
 *     <li>beacons that watch the edition ({@code ACTIVE} and {@code TRIGGERED}), in batches of 500, each batch
 *     in its own transaction with the rows locked;</li>
 *     <li>a beacon without a baseline (created when nothing was in stock) takes the best price as its baseline;</li>
 *     <li>condition met and the price is a new low for this beacon → {@code TRIGGERED}, notification recorded,
 *     email after the commit.</li>
 * </ol>
 * A triggered beacon keeps watching, so a further drop is reported again; the same or a higher price is not.
 */
@Slf4j
@Component
public class BeaconChecker {

    static final int BATCH = 500;
    private static final Set<BeaconStatus> WATCHING = Set.of(BeaconStatus.ACTIVE, BeaconStatus.TRIGGERED);

    private final BeaconRepository beaconRepository;
    private final OfferService offerService;
    private final BeaconAlerts alerts;
    private final ProcessedMessages processedMessages;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public BeaconChecker(BeaconRepository beaconRepository, OfferService offerService, BeaconAlerts alerts,
                         ProcessedMessages processedMessages, PlatformTransactionManager transactionManager, Clock clock) {
        this.beaconRepository = beaconRepository;
        this.offerService = offerService;
        this.alerts = alerts;
        this.processedMessages = processedMessages;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @RabbitListener(id = "price-changed-beacons", queues = RabbitTopology.PRICE_CHANGED_BEACONS_QUEUE, concurrency = "4")
    void onPriceChanged(PriceChangedMessage message) {
        MDC.put("traceId", message.messageId().toString());
        try {
            if (!processedMessages.runOnce(message.messageId(), () -> check(message.editionId()))) {
                log.info("Price change {} already checked, skipped", message.messageId());
            }
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * @return how many beacons fired
     */
    public int check(Long editionId) {
        List<Offer> offers = offerService.findMatchedOffers(editionId);
        if (offers.isEmpty() || !offers.get(0).isInStock()) {
            log.debug("Edition {} has no offer in stock, beacons not checked", editionId);
            return 0;
        }
        BigDecimal best = offers.get(0).getPrice();
        int fired = 0;
        long afterId = 0;
        while (true) {
            long from = afterId;
            Batch batch = transaction.execute(status -> checkBatch(editionId, from, best, offers));
            if (batch == null || batch.lastId() == null) {
                break;
            }
            fired += batch.fired();
            afterId = batch.lastId();
            if (batch.size() < BATCH) {
                break;
            }
        }
        if (fired > 0) {
            log.info("Edition {} at {}: {} beacons fired", editionId, best, fired);
        }
        return fired;
    }

    private Batch checkBatch(Long editionId, long afterId, BigDecimal best, List<Offer> offers) {
        List<Beacon> beacons = beaconRepository.findBatchForUpdate(editionId, WATCHING, afterId, PageRequest.of(0, BATCH));
        int fired = 0;
        for (Beacon beacon : beacons) {
            if (beacon.getBaselinePrice() == null) {
                beacon.setBaselinePrice(best);
            }
            if (!BeaconConditionEvaluator.isSatisfied(beacon, best) || !BeaconConditionEvaluator.isNewLow(beacon, best)) {
                continue;
            }
            BigDecimal was = beacon.getLastNotifiedPrice() != null ? beacon.getLastNotifiedPrice() : beacon.getBaselinePrice();
            beacon.setStatus(BeaconStatus.TRIGGERED);
            beacon.setLastTriggeredAt(clock.instant());
            beacon.setLastNotifiedPrice(best);
            alerts.fired(beacon, was, offers);
            fired++;
        }
        return new Batch(beacons.size(), beacons.isEmpty() ? null : beacons.get(beacons.size() - 1).getId(), fired);
    }

    private record Batch(int size, Long lastId, int fired) {
    }
}
