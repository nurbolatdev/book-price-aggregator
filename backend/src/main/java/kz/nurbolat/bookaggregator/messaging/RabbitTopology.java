package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.store.StoreCode;

import java.util.Locale;

/**
 * Queue names and routing keys shared by publishers, listeners and the topology.
 */
public final class RabbitTopology {

    public static final String PRICE_CHANGED_KEY = "price.changed";
    public static final String NOTIFY_EMAIL_KEY = "notify.email";
    public static final String PRICE_CHANGED_BEACONS_QUEUE = "kr.price-changed.beacons";
    public static final String PRICE_CHANGED_CACHE_QUEUE = "kr.price-changed.cache";
    public static final String NOTIFY_EMAIL_QUEUE = "kr.notify.email";

    // One queue per store, so that a slow or blocked store never holds up the others
    public static final String PARSE_FLIP_QUEUE = "kr.parse.flip";
    public static final String PARSE_KASPI_QUEUE = "kr.parse.kaspi";
    public static final String PARSE_MELOMAN_QUEUE = "kr.parse.meloman";
    public static final String PARSE_WILDBERRIES_QUEUE = "kr.parse.wildberries";
    public static final String PARSE_OZON_QUEUE = "kr.parse.ozon";

    /** Message header with the version of the message contract. */
    public static final String SCHEMA_VERSION_HEADER = "x-schema-version";
    public static final int SCHEMA_VERSION = 1;

    private RabbitTopology() {
    }

    /** {@code parse.request.flip} */
    public static String parseRoutingKey(StoreCode store) {
        return "parse.request." + store.name().toLowerCase(Locale.ROOT);
    }

    /** {@code kr.parse.flip} */
    public static String parseQueue(StoreCode store) {
        return "kr.parse." + store.name().toLowerCase(Locale.ROOT);
    }
}
