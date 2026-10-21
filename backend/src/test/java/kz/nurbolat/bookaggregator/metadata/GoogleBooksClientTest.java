package kz.nurbolat.bookaggregator.metadata;

import com.sun.net.httpserver.HttpServer;
import kz.nurbolat.bookaggregator.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the client against a local HTTP server; tests never call the real Google Books API.
 */
class GoogleBooksClientTest {

    private static final String VOLUMES = """
            {"items": [
              {"volumeInfo": {
                 "title": "Мастер и Маргарита",
                 "authors": ["Михаил Булгаков"],
                 "publisher": "АСТ",
                 "publishedDate": "2019-05-01",
                 "description": "Роман",
                 "pageCount": 480,
                 "language": "ru",
                 "industryIdentifiers": [{"type": "ISBN_10", "identifier": "0-306-40615-2"}],
                 "imageLinks": {"thumbnail": "http://books.google.com/cover.jpg"}},
               "saleInfo": {"listPrice": {"amount": 2500, "currencyCode": "RUB"}}},
              {"volumeInfo": {"authors": ["Без названия"]}}
            ]}
            """;

    private HttpServer server;
    private final AtomicReference<String> lastQuery = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void shouldMapMetadataAndIgnorePrices() {
        respond(200, VOLUMES, Duration.ZERO);

        List<BookMetadata> books = client(Duration.ofSeconds(2)).searchByText("мастер");

        assertThat(books).singleElement().isEqualTo(new BookMetadata(
                "Мастер и Маргарита", "Михаил Булгаков", "Роман", "9780306406157", "АСТ",
                2019, 480, "ru", "https://books.google.com/cover.jpg"));
        assertThat(lastQuery.get()).contains("langRestrict=ru");
    }

    @Test
    void shouldLookUpByIsbnWithoutLanguageRestriction() {
        respond(200, VOLUMES, Duration.ZERO);

        assertThat(client(Duration.ofSeconds(2)).findByIsbn("9780306406157")).isPresent();
        assertThat(lastQuery.get()).contains("q=isbn:9780306406157").doesNotContain("langRestrict");
    }

    @Test
    void shouldReturnEmptyResultWhenServerFails() {
        respond(500, "{}", Duration.ZERO);

        assertThat(client(Duration.ofSeconds(2)).searchByText("мастер")).isEmpty();
    }

    @Test
    void shouldReturnEmptyResultWhenServerIsTooSlow() {
        respond(200, VOLUMES, Duration.ofSeconds(2));

        assertThat(client(Duration.ofMillis(300)).searchByText("мастер")).isEmpty();
    }

    private GoogleBooksClient client(Duration timeout) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/books/v1";
        AppProperties properties = new AppProperties("http://localhost",
                new AppProperties.GoogleBooks(baseUrl, null, timeout), new AppProperties.Dev(false));
        return new GoogleBooksClient(RestClient.builder(), properties);
    }

    private void respond(int status, String body, Duration delay) {
        server.createContext("/books/v1/volumes", exchange -> {
            lastQuery.set(URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }
}
