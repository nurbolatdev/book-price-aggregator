package kz.nurbolat.bookaggregator.parser.core;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Downloads a store's robots.txt once a day and answers whether a URL may be fetched.
 * A missing robots.txt (4xx) allows everything; an unreachable one (5xx, network error)
 * forbids everything until the next attempt, as RFC 9309 requires.
 */
@Slf4j
@Component
public class RobotsTxtChecker {

    private static final Duration RETRY_AFTER_FAILURE = Duration.ofMinutes(10);

    private final ParserProperties properties;
    private final Clock clock;
    private final String productToken;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    private record Cached(RobotsRules rules, Instant expiresAt) {
    }

    public RobotsTxtChecker(ParserProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        // "KitapRadarBot/1.0 (...)" -> "KitapRadarBot"
        this.productToken = properties.userAgent().split("[/\\s]", 2)[0];
    }

    public boolean isAllowed(String url) {
        URI uri = URI.create(url);
        String origin = uri.getScheme() + "://" + uri.getRawAuthority();
        String pathAndQuery = (uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath())
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        return rulesFor(origin).allows(pathAndQuery);
    }

    private RobotsRules rulesFor(String origin) {
        Instant now = clock.instant();
        Cached cached = cache.get(origin);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.rules();
        }
        Cached loaded = load(origin, now);
        cache.put(origin, loaded);
        return loaded.rules();
    }

    private Cached load(String origin, Instant now) {
        String url = origin + "/robots.txt";
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(properties.userAgent())
                    .timeout((int) properties.timeout().toMillis())
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .execute();
            int status = response.statusCode();
            if (status >= 500) {
                log.warn("robots.txt unavailable ({}): {}; all paths disallowed for now", status, url);
                return new Cached(RobotsRules.DISALLOW_ALL, now.plus(RETRY_AFTER_FAILURE));
            }
            RobotsRules rules = status >= 400
                    ? RobotsRules.ALLOW_ALL
                    : RobotsRules.parse(response.body(), productToken);
            log.info("robots.txt loaded: {} status={} rules={}", url, status, rules.rules().size());
            return new Cached(rules, now.plus(properties.robotsTtl()));
        } catch (IOException e) {
            log.warn("robots.txt request failed: {} {}; all paths disallowed for now", url, e.toString());
            return new Cached(RobotsRules.DISALLOW_ALL, now.plus(RETRY_AFTER_FAILURE));
        }
    }
}
