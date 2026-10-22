package kz.nurbolat.bookaggregator.parser.core;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

/**
 * The single entry point for HTTP requests to stores (docs/05-parsing-policy.md):
 * pages are reused from the cache for an hour; every real request passes a per-store rate limit
 * ({@code stores.min_delay_ms}), a circuit breaker and up to two retries on 5xx and network errors.
 * Paths forbidden by robots.txt are never requested. 403, 429 and captcha pages stop the store
 * instead of being retried.
 */
@Slf4j
@Component
public class HtmlFetcher {

    // Catalog pages of Meloman are over 5 MB
    private static final int MAX_BODY_BYTES = 8 * 1024 * 1024;
    // reCAPTCHA, hCaptcha, Yandex SmartCaptcha, Cloudflare Turnstile, Wildberries antibot
    private static final String CAPTCHA_WIDGETS =
            "[data-sitekey], [data-site-key], .g-recaptcha, .h-captcha, .smart-captcha, .cf-turnstile, iframe[src*=captcha]";

    private final ParserProperties properties;
    private final PageCache pageCache;
    private final RobotsTxtChecker robotsTxtChecker;
    private final StoreResilience resilience;

    public HtmlFetcher(ParserProperties properties, PageCache pageCache, RobotsTxtChecker robotsTxtChecker,
                       StoreService storeService,
                       CircuitBreakerRegistry circuitBreakers, RateLimiterRegistry rateLimiters,
                       RetryRegistry retries) {
        this.properties = properties;
        this.pageCache = pageCache;
        this.robotsTxtChecker = robotsTxtChecker;
        this.resilience = new StoreResilience(properties, storeService, circuitBreakers, rateLimiters, retries);
    }

    /**
     * @throws StoreBlockedException      the store answered 403, 429 or a captcha
     * @throws StoreUnavailableException  the store is paused by its circuit breaker
     * @throws DisallowedByRobotsException the path is forbidden by the store's robots.txt
     * @throws PageNotFoundException      404 / 410
     * @throws FetchException             any other failure after retries
     */
    public Document get(StoreCode store, String url) {
        Optional<String> cached = pageCache.get(url);
        if (cached.isPresent()) {
            log.debug("Page from cache: store={} url={}", store, url);
            return Jsoup.parse(cached.get(), url);
        }
        if (!robotsTxtChecker.isAllowed(url)) {
            log.info("Skipped by robots.txt: store={} url={}", store, url);
            throw new DisallowedByRobotsException(url);
        }

        String html = resilience.call(store, () -> download(store, url));
        pageCache.put(url, html, properties.htmlCacheTtl());
        return Jsoup.parse(html, url);
    }

    private String download(StoreCode store, String url) {
        long started = System.nanoTime();
        Connection.Response response;
        try {
            response = Jsoup.connect(url)
                    .userAgent(properties.userAgent())
                    .timeout((int) properties.timeout().toMillis())
                    .maxBodySize(MAX_BODY_BYTES)
                    .ignoreHttpErrors(true)
                    .execute();
        } catch (IOException e) {
            log.warn("Request failed: store={} url={} timeMs={} error={}", store, url, elapsedMs(started), e.toString());
            throw new RetryableFetchException("Request to %s failed: %s".formatted(url, e.getMessage()), e);
        }

        int status = response.statusCode();
        log.info("Fetched store={} url={} status={} timeMs={}", store, url, status, elapsedMs(started));
        if (status == 403 || status == 429) {
            throw new StoreBlockedException(store, url, "HTTP " + status);
        }
        if (status == 404 || status == 410) {
            throw new PageNotFoundException(url, status);
        }
        if (status >= 500) {
            throw new RetryableFetchException("HTTP %d from %s".formatted(status, url));
        }
        if (status >= 400) {
            throw new FetchException("HTTP %d from %s".formatted(status, url));
        }
        String body = response.body();
        if (looksLikeCaptcha(body)) {
            throw new StoreBlockedException(store, url, "captcha page");
        }
        return body;
    }

    /**
     * A challenge page shows a captcha widget or a form with a captcha in it. Words alone do not count:
     * Magento stores mention "captcha" in the login settings of every page.
     */
    static boolean looksLikeCaptcha(String html) {
        if (!html.toLowerCase(Locale.ROOT).contains("captcha") && !html.contains("site-key") && !html.contains("sitekey")) {
            return false;
        }
        Document page = Jsoup.parse(html);
        return !page.select(CAPTCHA_WIDGETS).isEmpty()
                || page.select("form").stream().anyMatch(form -> form.html().toLowerCase(Locale.ROOT).contains("captcha"));
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
