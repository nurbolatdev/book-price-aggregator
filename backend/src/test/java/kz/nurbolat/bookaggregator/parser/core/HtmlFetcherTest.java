package kz.nurbolat.bookaggregator.parser.core;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Fetcher against a local WireMock server; no request leaves the machine.
 */
@WireMockTest
class HtmlFetcherTest {

    private static final String USER_AGENT = "KitapRadarBot/1.0 (+https://github.com/nurbolatdev/book-price-aggregator; educational project)";
    private static final String PAGE = "<html><body><h1>Книга</h1></body></html>";

    private final InMemoryPageCache cache = new InMemoryPageCache();
    private HtmlFetcher fetcher;
    private String baseUrl;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMock) {
        baseUrl = wireMock.getHttpBaseUrl();
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(aResponse().withStatus(404)));
        fetcher = fetcher(1);
    }

    @Test
    void shouldSendHonestUserAgentAndParsePage() {
        stubFor(get(urlEqualTo("/book")).willReturn(ok(PAGE)));

        Document page = fetcher.get(StoreCode.FLIP, baseUrl + "/book");

        assertThat(page.selectFirst("h1").text()).isEqualTo("Книга");
        verify(getRequestedFor(urlEqualTo("/book")).withHeader("User-Agent", equalTo(USER_AGENT)));
    }

    @Test
    void shouldServeSecondRequestFromCache() {
        stubFor(get(urlEqualTo("/book")).willReturn(ok(PAGE)));

        fetcher.get(StoreCode.FLIP, baseUrl + "/book");
        Document second = fetcher.get(StoreCode.FLIP, baseUrl + "/book");

        assertThat(second.selectFirst("h1").text()).isEqualTo("Книга");
        verify(exactly(1), getRequestedFor(urlEqualTo("/book")));
    }

    @Test
    void shouldRetryWhenStoreAnswers503() {
        stubFor(get(urlEqualTo("/flaky")).inScenario("flaky").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503)).willSetStateTo("recovered"));
        stubFor(get(urlEqualTo("/flaky")).inScenario("flaky").whenScenarioStateIs("recovered")
                .willReturn(ok(PAGE)));

        Document page = fetcher.get(StoreCode.FLIP, baseUrl + "/flaky");

        assertThat(page.selectFirst("h1")).isNotNull();
        verify(exactly(2), getRequestedFor(urlEqualTo("/flaky")));
    }

    @Test
    void shouldGiveUpAfterTwoRetries() {
        stubFor(get(urlEqualTo("/down")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> fetcher.get(StoreCode.FLIP, baseUrl + "/down"))
                .isInstanceOf(RetryableFetchException.class);
        verify(exactly(3), getRequestedFor(urlEqualTo("/down")));
    }

    @Test
    void shouldNotRetryAndStopStoreWhenRateLimitedWith429() {
        stubFor(get(urlEqualTo("/limited")).willReturn(aResponse().withStatus(429)));
        stubFor(get(urlEqualTo("/other")).willReturn(ok(PAGE)));

        assertThatThrownBy(() -> fetcher.get(StoreCode.FLIP, baseUrl + "/limited"))
                .isInstanceOf(StoreBlockedException.class);
        assertThatThrownBy(() -> fetcher.get(StoreCode.FLIP, baseUrl + "/other"))
                .isInstanceOf(StoreUnavailableException.class);

        verify(exactly(1), getRequestedFor(urlEqualTo("/limited")));
        verify(exactly(0), getRequestedFor(urlEqualTo("/other")));
    }

    @Test
    void shouldTreatCaptchaPageAsBlock() {
        stubFor(get(urlEqualTo("/challenge")).willReturn(ok(
                "<html><body><form action='/check'><div class='smart-captcha'></div></form></body></html>")));

        assertThatThrownBy(() -> fetcher.get(StoreCode.FLIP, baseUrl + "/challenge"))
                .isInstanceOf(StoreBlockedException.class);
    }

    @Test
    void shouldNotTakePageMentioningCaptchaInScriptsForChallenge() {
        // Magento writes its login captcha settings into every page
        String page = "<html><body><form action='/cart/add'><button>Купить</button></form>"
                + "<script>{\"sectionNames\": [\"cart\", \"captcha\"]}</script><h1>Книга</h1></body></html>";

        assertThat(HtmlFetcher.looksLikeCaptcha(page)).isFalse();
        assertThat(HtmlFetcher.looksLikeCaptcha("<html><body><b id='s-key' data-site-key='f0'></b></body></html>")).isTrue();
    }

    @Test
    void shouldOpenCircuitBreakerWhenHalfOfRequestsFail() {
        stubFor(get(urlEqualTo("/broken")).willReturn(aResponse().withStatus(502)));
        stubFor(get(urlEqualTo("/fine")).willReturn(ok(PAGE)));

        // 3 failing calls x 3 attempts = 9 recorded failures, the 10th call opens the breaker
        for (int i = 0; i < 4; i++) {
            try {
                fetcher.get(StoreCode.FLIP, baseUrl + "/broken");
            } catch (FetchException expected) {
                // keep failing
            }
        }

        assertThatThrownBy(() -> fetcher.get(StoreCode.FLIP, baseUrl + "/fine"))
                .isInstanceOf(StoreUnavailableException.class);
        verify(exactly(0), getRequestedFor(urlEqualTo("/fine")));
    }

    @Test
    void shouldNotRetryMissingPage() {
        stubFor(get(urlEqualTo("/gone")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> fetcher.get(StoreCode.FLIP, baseUrl + "/gone"))
                .isInstanceOf(PageNotFoundException.class);
        verify(exactly(1), getRequestedFor(urlEqualTo("/gone")));
    }

    @Test
    void shouldNotRequestPathDisallowedByRobotsTxt() {
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(ok("""
                User-agent: *
                Disallow: /cart
                Disallow: /search*page
                """)));
        stubFor(get(urlEqualTo("/search?search=abc&page=2")).willReturn(ok(PAGE)));
        stubFor(get(urlEqualTo("/search?search=abc")).willReturn(ok(PAGE)));
        HtmlFetcher politeFetcher = fetcher(1);

        assertThatThrownBy(() -> politeFetcher.get(StoreCode.FLIP, baseUrl + "/search?search=abc&page=2"))
                .isInstanceOf(DisallowedByRobotsException.class);
        assertThat(politeFetcher.get(StoreCode.FLIP, baseUrl + "/search?search=abc")).isNotNull();

        verify(exactly(0), getRequestedFor(urlEqualTo("/search?search=abc&page=2")));
        verify(exactly(1), getRequestedFor(urlEqualTo("/robots.txt")));
    }

    @Test
    void shouldKeepConfiguredPauseBetweenRequestsToOneStore() {
        HtmlFetcher slowStoreFetcher = fetcher(300);
        stubFor(get(urlEqualTo("/a")).willReturn(ok(PAGE)));
        stubFor(get(urlEqualTo("/b")).willReturn(ok(PAGE)));
        stubFor(get(urlEqualTo("/c")).willReturn(ok(PAGE)));

        long started = System.nanoTime();
        slowStoreFetcher.get(StoreCode.KASPI, baseUrl + "/a");
        slowStoreFetcher.get(StoreCode.KASPI, baseUrl + "/b");
        slowStoreFetcher.get(StoreCode.KASPI, baseUrl + "/c");
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        assertThat(elapsedMs).isGreaterThanOrEqualTo(550);
    }

    private HtmlFetcher fetcher(int minDelayMs) {
        ParserProperties properties = new ParserProperties(USER_AGENT, Duration.ofSeconds(2), Duration.ofHours(1),
                Duration.ofHours(24), 200, 50, 5, Duration.ofMillis(10), Duration.ofMinutes(30), Duration.ofSeconds(5));
        StoreService storeService = mock(StoreService.class);
        for (StoreCode code : StoreCode.values()) {
            Store store = new Store();
            store.setCode(code);
            store.setMinDelayMs(minDelayMs);
            when(storeService.getByCode(code)).thenReturn(store);
        }
        RobotsTxtChecker robots = new RobotsTxtChecker(properties, Clock.systemUTC());
        return new HtmlFetcher(properties, cache, robots, storeService, CircuitBreakerRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults(), RetryRegistry.ofDefaults());
    }

    static class InMemoryPageCache implements PageCache {

        private final Map<String, String> pages = new ConcurrentHashMap<>();

        @Override
        public Optional<String> get(String url) {
            return Optional.ofNullable(pages.get(url));
        }

        @Override
        public void put(String url, String html, Duration ttl) {
            pages.put(url, html);
        }
    }
}
