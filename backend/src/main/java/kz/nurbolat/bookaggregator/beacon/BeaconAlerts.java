package kz.nurbolat.bookaggregator.beacon;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.common.text.Emails;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.notification.EmailPublisher;
import kz.nurbolat.bookaggregator.notification.EmailType;
import kz.nurbolat.bookaggregator.notification.NotificationService;
import kz.nurbolat.bookaggregator.offer.Offer;
import kz.nurbolat.bookaggregator.user.NotifyFrequency;
import kz.nurbolat.bookaggregator.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What happens when a beacon fires: the notification is recorded, and the email goes out right away
 * ({@code INSTANT}), waits for the daily digest ({@code DAILY}) or is not sent because the address is not
 * confirmed. Runs inside the checker's transaction; the email leaves after its commit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class BeaconAlerts {

    static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";

    private final NotificationService notificationService;
    private final EmailPublisher emailPublisher;

    /**
     * @param was    the price the user saw last: the previous reported price or the baseline
     * @param offers matched offers of the edition, the best one first
     */
    void fired(Beacon beacon, BigDecimal was, List<Offer> offers) {
        User user = beacon.getUser();
        Map<String, Object> payload = payload(beacon, was, offers);
        if (!user.isEmailVerified()) {
            notificationService.fail(user, beacon, payload, EMAIL_NOT_VERIFIED);
            log.info("Beacon {} fired, email not sent: {} is not confirmed", beacon.getId(), Emails.mask(user.getEmail()));
            return;
        }
        notificationService.queue(user, beacon, payload);
        if (user.getNotifyFrequency() == NotifyFrequency.INSTANT) {
            emailPublisher.publish(EmailType.PRICE_DROP, user.getEmail(), payload);
        }
        log.info("Beacon {} fired at {}, email {}", beacon.getId(), offers.get(0).getPrice(),
                user.getNotifyFrequency() == NotifyFrequency.INSTANT ? "queued" : "kept for the daily digest");
    }

    /**
     * What the email shows, kept in {@code notifications.payload}: the digest is built from it later.
     */
    static Map<String, Object> payload(Beacon beacon, BigDecimal was, List<Offer> offers) {
        Edition edition = beacon.getEdition();
        Book book = edition.getBook();
        Offer best = offers.get(0);
        Map<String, Object> payload = new HashMap<>();
        payload.put("beaconId", beacon.getId());
        payload.put("editionId", edition.getId());
        payload.put("title", book.getTitle());
        payload.put("author", book.getAuthor());
        payload.put("coverUrl", edition.getCoverUrl());
        payload.put("conditionType", beacon.getConditionType().name());
        payload.put("store", best.getStore().getCode().name());
        payload.put("storeName", best.getStore().getName());
        payload.put("offerUrl", best.getUrl());
        payload.put("oldPrice", was);
        payload.put("newPrice", best.getPrice());
        payload.put("pricesCollectedAt", best.getLastSeenAt() == null ? null : best.getLastSeenAt().toString());
        payload.put("otherOffers", offers.stream().skip(1).map(offer -> {
            Map<String, Object> other = new HashMap<>();
            other.put("storeName", offer.getStore().getName());
            other.put("price", offer.getPrice());
            other.put("inStock", offer.isInStock());
            other.put("url", offer.getUrl());
            return other;
        }).toList());
        return payload;
    }
}
