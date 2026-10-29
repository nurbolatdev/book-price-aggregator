package kz.nurbolat.bookaggregator.notification;

import kz.nurbolat.bookaggregator.common.text.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the payload of a weekly digest notification into what the template shows: prices as text, absolute links
 * and the subject. Payload numbers may come back from JSON as any {@link Number}, so they are read through their
 * text.
 */
public final class WeeklyDigestEmail {

    private static final ZoneId ALMATY = ZoneId.of("Asia/Almaty");
    private static final DateTimeFormatter COLLECTED_AT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private WeeklyDigestEmail() {
    }

    /** {@code Подборка недели: 8 книг подешевели до −32%} */
    public static String subject(Map<String, Object> payload) {
        List<Map<String, Object>> items = items(payload);
        int books = items.size();
        int maxDrop = items.stream().mapToInt(item -> number(item.get("dropPercent"))).max().orElse(0);
        int lastTwo = books % 100;
        int last = books % 10;
        String word = lastTwo >= 11 && lastTwo <= 14 ? "книг"
                : last == 1 ? "книга"
                : last >= 2 && last <= 4 ? "книги"
                : "книг";
        String verb = last == 1 && lastTwo != 11 ? "подешевела" : "подешевели";
        return "Подборка недели: " + books + " " + word + " " + verb + " до −" + maxDrop + "%";
    }

    public static Map<String, Object> model(Map<String, Object> payload, String baseUrl, String name) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", name);
        model.put("periodText", payload.get("periodText"));
        model.put("dealsUrl", baseUrl + "/deals");
        model.put("settingsUrl", baseUrl + "/profile");
        Object collected = payload.get("pricesCollectedAt");
        model.put("collectedAtText", collected == null ? null
                : COLLECTED_AT.format(Instant.parse(collected.toString()).atZone(ALMATY)));
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Map<String, Object> item : items(payload)) {
            Map<String, Object> card = new HashMap<>();
            card.put("title", item.get("title"));
            card.put("author", item.get("author"));
            card.put("coverUrl", item.get("coverUrl"));
            card.put("storeName", item.get("storeName"));
            card.put("priceText", Money.format(new BigDecimal(item.get("price").toString())));
            card.put("dropPercent", number(item.get("dropPercent")));
            card.put("editionUrl", baseUrl + "/editions/" + item.get("editionId"));
            cards.add(card);
        }
        model.put("items", cards);
        return model;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> payload) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (payload.get("items") instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map) {
                    items.add((Map<String, Object>) map);
                }
            }
        }
        return items;
    }

    private static int number(Object value) {
        return value == null ? 0 : new BigDecimal(value.toString()).intValue();
    }
}
