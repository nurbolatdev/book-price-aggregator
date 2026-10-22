package kz.nurbolat.bookaggregator.parser.core;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PreDestroy;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Loads pages that are empty without JavaScript in headless Chromium (Playwright), under the same
 * rules as {@link HtmlFetcher}: robots.txt, the store's rate limit and circuit breaker, the honest
 * User-Agent and the page cache. Nothing hides that this is an automated browser: no stealth
 * plugins, no fingerprint changes.
 * <p>
 * One browser per application, started on first use; every page gets its own context, closed
 * right after, and images, fonts and media are not downloaded. Playwright objects are not
 * thread-safe, so pages are rendered one at a time.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.parser.browser", name = "enabled", havingValue = "true")
public class BrowserFetcher {

    private static final Set<String> SKIPPED_RESOURCES = Set.of("image", "font", "media");
    // Rendered pages share the cache with plain ones; the prefix keeps the two versions of a URL apart
    private static final String CACHE_PREFIX = "rendered:";

    private final ParserProperties properties;
    private final BrowserProperties browserProperties;
    private final PageCache pageCache;
    private final RobotsTxtChecker robotsTxtChecker;
    private final StoreResilience resilience;
    private final ReentrantLock lock = new ReentrantLock();

    private Playwright playwright;
    private Browser browser;

    public BrowserFetcher(ParserProperties properties, BrowserProperties browserProperties, PageCache pageCache,
                          RobotsTxtChecker robotsTxtChecker, StoreService storeService,
                          CircuitBreakerRegistry circuitBreakers, RateLimiterRegistry rateLimiters,
                          RetryRegistry retries) {
        this.properties = properties;
        this.browserProperties = browserProperties;
        this.pageCache = pageCache;
        this.robotsTxtChecker = robotsTxtChecker;
        this.resilience = new StoreResilience(properties, storeService, circuitBreakers, rateLimiters, retries);
    }

    /**
     * {@link #render(StoreCode, String, String)} without waiting for particular content.
     */
    public String render(StoreCode store, String url) {
        return render(store, url, null);
    }

    /**
     * @param waitForSelector CSS selector of the content the page renders by script (e.g. the price);
     *                        {@code null} to take the page once its HTML is loaded
     * @return HTML of the page after scripts ran
     * @throws StoreBlockedException       the store answered 403, 429 or a captcha
     * @throws StoreUnavailableException   the store is paused by its circuit breaker
     * @throws DisallowedByRobotsException the path is forbidden by the store's robots.txt
     * @throws PageNotFoundException       404 / 410
     * @throws FetchException              any other failure after retries
     */
    public String render(StoreCode store, String url, String waitForSelector) {
        Optional<String> cached = pageCache.get(CACHE_PREFIX + url);
        if (cached.isPresent()) {
            log.debug("Rendered page from cache: store={} url={}", store, url);
            return cached.get();
        }
        if (!robotsTxtChecker.isAllowed(url)) {
            log.info("Skipped by robots.txt: store={} url={}", store, url);
            throw new DisallowedByRobotsException(url);
        }

        String html = resilience.call(store, () -> load(store, url, waitForSelector));
        pageCache.put(CACHE_PREFIX + url, html, properties.htmlCacheTtl());
        return html;
    }

    private String load(StoreCode store, String url, String waitForSelector) {
        lock.lock();
        try {
            long started = System.nanoTime();
            BrowserContext context = browser().newContext(new Browser.NewContextOptions()
                    .setUserAgent(properties.userAgent()));
            try {
                context.route("**/*", route -> {
                    if (SKIPPED_RESOURCES.contains(route.request().resourceType())) {
                        route.abort();
                    } else {
                        route.resume();
                    }
                });
                Page page = context.newPage();
                page.setDefaultTimeout(browserProperties.timeout().toMillis());
                Response response = page.navigate(url, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(browserProperties.timeout().toMillis()));
                int status = response == null ? 200 : response.status();
                log.info("Rendered store={} url={} status={} timeMs={}", store, url, status, elapsedMs(started));
                checkStatus(store, url, status);
                if (waitForSelector != null) {
                    page.waitForSelector(waitForSelector);
                }
                String html = page.content();
                if (HtmlFetcher.looksLikeCaptcha(html)) {
                    throw new StoreBlockedException(store, url, "captcha page");
                }
                return html;
            } finally {
                context.close();
            }
        } catch (PlaywrightException e) {
            log.warn("Rendering failed: store={} url={} error={}", store, url, e.getMessage());
            throw new RetryableFetchException("Rendering %s failed: %s".formatted(url, e.getMessage()), e);
        } finally {
            lock.unlock();
        }
    }

    private static void checkStatus(StoreCode store, String url, int status) {
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
    }

    // Called under the lock
    private Browser browser() {
        if (browser == null) {
            playwright = Playwright.create();
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
            if (browserProperties.executablePath() != null && !browserProperties.executablePath().isBlank()) {
                options.setExecutablePath(Path.of(browserProperties.executablePath()));
            }
            browser = playwright.chromium().launch(options);
            log.info("Started headless Chromium {}", browser.version());
        }
        return browser;
    }

    @PreDestroy
    void close() {
        lock.lock();
        try {
            if (playwright != null) {
                playwright.close();
                playwright = null;
                browser = null;
            }
        } finally {
            lock.unlock();
        }
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
