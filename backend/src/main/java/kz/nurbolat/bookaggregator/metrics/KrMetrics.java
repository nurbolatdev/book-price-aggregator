package kz.nurbolat.bookaggregator.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The application's own Micrometer metrics, all named {@code kr.*}; they are served at
 * {@code /actuator/prometheus} and feed the admin KPI (cache hit share).
 */
@Component
@RequiredArgsConstructor
public class KrMetrics {

    public static final String SEARCH_REQUESTS = "kr.search.requests";

    private final MeterRegistry registry;

    /** @param hit the page came from the Redis cache */
    public void searchRequest(boolean hit) {
        registry.counter(SEARCH_REQUESTS, "cache", hit ? "hit" : "miss").increment();
    }

    public void parserRun(String store, String status, Duration duration) {
        Timer.builder("kr.parser.run.duration").tags("store", store, "status", status).register(registry)
                .record(duration);
    }

    public void parserOffersUpserted(String store, int count) {
        registry.counter("kr.parser.offers.upserted", "store", store).increment(count);
    }

    public void parserPricesChanged(String store, int count) {
        registry.counter("kr.parser.prices.changed", "store", store).increment(count);
    }

    public void beaconTriggered(int count) {
        registry.counter("kr.beacon.triggered").increment(count);
    }

    /** @param result {@code sent} or {@code failed} */
    public void mail(String type, String result) {
        registry.counter("kr.mail.sent", "type", type, "result", result).increment();
    }
}
