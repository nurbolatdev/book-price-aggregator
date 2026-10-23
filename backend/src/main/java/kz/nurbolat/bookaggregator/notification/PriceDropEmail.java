package kz.nurbolat.bookaggregator.notification;

import kz.nurbolat.bookaggregator.common.text.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the payload of a beacon notification into what the price-drop and digest templates show: prices as
 * text ({@code 3 150 ₸}), the saving, absolute links and the time prices were collected in Almaty time. Payload
 * numbers may come back from JSON as any {@link Number}, so they are read through their text.
 */
public final class PriceDropEmail {

    private static final ZoneId ALMATY = ZoneId.of("Asia/Almaty");
    private static final DateTimeFormatter COLLECTED_AT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private PriceDropEmail() {
    }

    /** {@code Цена упала: «Мастер и Маргарита» — 3 150 ₸} */
    public static String subject(Map<String, Object> payload) {
        return "Цена упала: «" + payload.get("title") + "» — " + Money.format(decimal(payload.get("newPrice")));
    }

    public static Map<String, Object> model(Map<String, Object> payload, String baseUrl) {
        Map<String, Object> model = item(payload, baseUrl);
        model.put("beaconsUrl", baseUrl + "/beacons");
        model.put("settingsUrl", baseUrl + "/profile");
        Object collected = payload.get("pricesCollectedAt");
        model.put("collectedAtText", collected == null ? null
                : COLLECTED_AT.format(Instant.parse(collected.toString()).atZone(ALMATY)));
        List<Map<String, Object>> others = new ArrayList<>();
        if (payload.get("otherOffers") instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> offer) {
                    Map<String, Object> other = new HashMap<>();
                    other.put("storeName", offer.get("storeName"));
                    other.put("priceText", Money.format(decimal(offer.get("price"))));
                    other.put("inStock", Boolean.TRUE.equals(offer.get("inStock")));
                    other.put("url", offer.get("url"));
                    others.add(other);
                }
            }
        }
        model.put("otherOffers", others);
        return model;
    }

    /**
     * One book of an email: title, store, prices and links. The old price and the saving are given only when the
     * price really went down from it.
     */
    public static Map<String, Object> item(Map<String, Object> payload, String baseUrl) {
        BigDecimal newPrice = decimal(payload.get("newPrice"));
        BigDecimal oldPrice = decimal(payload.get("oldPrice"));
        Map<String, Object> item = new HashMap<>();
        item.put("title", payload.get("title"));
        item.put("author", payload.get("author"));
        item.put("coverUrl", payload.get("coverUrl"));
        item.put("storeName", payload.get("storeName"));
        item.put("offerUrl", payload.get("offerUrl"));
        item.put("editionUrl", baseUrl + "/editions/" + payload.get("editionId"));
        item.put("newPriceText", Money.format(newPrice));
        if (oldPrice != null && oldPrice.compareTo(newPrice) > 0) {
            BigDecimal saving = oldPrice.subtract(newPrice);
            item.put("oldPriceText", Money.format(oldPrice));
            item.put("savingText", Money.format(saving));
            item.put("savingPercent", saving.multiply(BigDecimal.valueOf(100)).divide(oldPrice, 0, RoundingMode.DOWN).intValue());
        }
        return item;
    }

    private static BigDecimal decimal(Object value) {
        return value == null ? null : new BigDecimal(value.toString());
    }
}
