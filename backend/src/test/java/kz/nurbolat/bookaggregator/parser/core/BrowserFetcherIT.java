package kz.nurbolat.bookaggregator.parser.core;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import kz.nurbolat.bookaggregator.parser.core.HtmlFetcherTest.InMemoryPageCache;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Real headless Chromium against a local WireMock server; no request leaves the machine.
 * Needs the Playwright browser installed, so it runs only with {@code ./mvnw verify -Pbrowser}.
 */
@Tag("browser")
@WireMockTest
class BrowserFetcherIT {

    private static final String USER_AGENT = "KitapRadarBot/1.0 (+https://github.com/nurbolatdev/book-price-aggregator; educational project)";

    private final InMemoryPageCache cache = new InMemoryPageCache();
    private BrowserFetcher fetcher;
    private String baseUrl;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMock) throws IOException {
        baseUrl = wireMock.getHttpBaseUrl();
        stubFor(get(urlEqualTo("/robots.txt")).willReturn(aResponse().withStatus(200)
                .withBody("User-agent: *\nDisallow: /private\n")));
        stubFor(get(urlEqualTo("/product")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody(resource("/browser/js-rendered-product.html"))));
        stubFor(get(urlEqualTo("/cover.png")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "image/png").withBody(new byte[]{1, 2, 3})));
        fetcher = fetcher();
    }

    @AfterEach
    void tearDown() {
        fetcher.close();
    }

    @Test
    void shouldReturnContentRenderedByScriptWithHonestUserAgent() {
        String html = fetcher.render(StoreCode.WILDBERRIES, baseUrl + "/product", ".price");

        Document page = Jsoup.parse(html);
        assertThat(page.selectFirst("h1.title").text()).isEqualTo("Пикник на обочине");
        assertThat(page.selectFirst(".price").text()).isEqualTo("3 450 ₸");
        verify(getRequestedFor(urlEqualTo("/product")).withHeader("User-Agent", equalTo(USER_AGENT)));
    }

    @Test
    void shouldNotDownloadImages() {
        fetcher.render(StoreCode.WILDBERRIES, baseUrl + "/product", ".price");

        verify(exactly(0), getRequestedFor(urlEqualTo("/cover.png")));
    }

    @Test
    void shouldServeSecondRenderFromCache() {
        fetcher.render(StoreCode.WILDBERRIES, baseUrl + "/product", ".price");
        String second = fetcher.render(StoreCode.WILDBERRIES, baseUrl + "/product", ".price");

        assertThat(second).contains("Пикник на обочине");
        verify(exactly(1), getRequestedFor(urlEqualTo("/product")));
    }

    @Test
    void shouldStopStoreWhenItAnswers429() {
        stubFor(get(urlEqualTo("/limited")).willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> fetcher.render(StoreCode.OZON, baseUrl + "/limited"))
                .isInstanceOf(StoreBlockedException.class);
        assertThatThrownBy(() -> fetcher.render(StoreCode.OZON, baseUrl + "/product"))
                .isInstanceOf(StoreUnavailableException.class);
        verify(exactly(1), getRequestedFor(urlEqualTo("/limited")));
        verify(exactly(0), getRequestedFor(urlEqualTo("/product")));
    }

    @Test
    void shouldNotOpenPathForbiddenByRobotsTxt() {
        assertThatThrownBy(() -> fetcher.render(StoreCode.KASPI, baseUrl + "/private/page"))
                .isInstanceOf(DisallowedByRobotsException.class);
        verify(exactly(0), getRequestedFor(urlEqualTo("/private/page")));
    }

    private BrowserFetcher fetcher() {
        ParserProperties properties = new ParserProperties(USER_AGENT, Duration.ofSeconds(5), Duration.ofHours(1),
                Duration.ofHours(24), 200, 50, 5, Duration.ofMillis(10), Duration.ofMinutes(30), Duration.ofSeconds(5));
        StoreService storeService = mock(StoreService.class);
        for (StoreCode code : StoreCode.values()) {
            Store store = new Store();
            store.setCode(code);
            store.setMinDelayMs(1);
            when(storeService.getByCode(code)).thenReturn(store);
        }
        return new BrowserFetcher(properties, new BrowserProperties(true, Duration.ofSeconds(15), null), cache,
                new RobotsTxtChecker(properties, Clock.systemUTC()), storeService, CircuitBreakerRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults(), RetryRegistry.ofDefaults());
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = BrowserFetcherIT.class.getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
