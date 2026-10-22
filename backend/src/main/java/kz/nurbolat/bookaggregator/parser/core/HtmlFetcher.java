package kz.nurbolat.bookaggregator.parser.core;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

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

    private static final int MAX_BODY_BYTES = 5 * 1024 * 1024;

    private final ParserProperties properties;
    private final PageCache pageCache;
    private final RobotsTxtChecker robotsTxtChecker;
    private final StoreService storeService;
    private final CircuitBreakerRegistry circuitBreakers;
    private final RateLimiterRegistry rateLimiters;
    private final RetryRegistry retries;

    public HtmlFetcher(ParserProperties properties, PageCache pageCache, RobotsTxtChecker robotsTxtChecker,
                       StoreService storeService,
                       CircuitBreakerRegistry circuitBreakers, RateLimiterRegistry rateLimiters,
                       RetryRegistry retries) {
        this.properties = properties;
        this.pageCache = pageCache;
        this.robotsTxtChecker = robotsTxtChecker;
        this.storeService = storeService;
        this.circuitBreakers = circuitBreakers;
        this.rateLimiters = rateLimiters;
        this.retries = retries;
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

        CircuitBreaker circuitBreaker = circuitBreaker(store);
        Supplier<String> request = () -> download(store, url);
        Supplier<String> guarded = Retry.decorateSupplier(retry(store),
                CircuitBreaker.decorateSupplier(circuitBreaker,
                        RateLimiter.decorateSupplier(rateLimiter(store), request)));
        String html;
        try {
            html = guarded.get();
        } catch (StoreBlockedException e) {
            // Stop all further requests to this store for the open period
            circuitBreaker.transitionToOpenState();
            throw e;
        } catch (CallNotPermittedException | RequestNotPermitted e) {
            throw new StoreUnavailableException(store, e);
        }
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

    // Normal store pages never mention captchas; a challenge page is a form that does
    static boolean looksLikeCaptcha(String html) {
        String lower = html.toLowerCase(Locale.ROOT);
        return lower.contains("<form") && lower.contains("captcha");
    }

    private CircuitBreaker circuitBreaker(StoreCode store) {
        return circuitBreakers.circuitBreaker("store-" + store, () -> CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(properties.circuitBreakerOpenDuration())
                .recordExceptions(RetryableFetchException.class, StoreBlockedException.class)
                .ignoreExceptions(PageNotFoundException.class)
                .build());
    }

    private RateLimiter rateLimiter(StoreCode store) {
        Duration minDelay = Duration.ofMillis(Math.max(1, storeService.getByCode(store).getMinDelayMs()));
        return rateLimiters.rateLimiter("store-" + store, () -> RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(minDelay)
                .timeoutDuration(properties.rateLimitMaxWait())
                .build());
    }

    private Retry retry(StoreCode store) {
        return retries.retry("store-" + store, () -> RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(properties.retryWait())
                .retryExceptions(RetryableFetchException.class)
                .build());
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
