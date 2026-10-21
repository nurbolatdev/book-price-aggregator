package kz.nurbolat.bookaggregator.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import kz.nurbolat.bookaggregator.common.text.IsbnUtils;
import kz.nurbolat.bookaggregator.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Google Books volumes API. Used to fill the catalog with metadata; prices from it are ignored.
 * Failures never propagate: the caller gets an empty result and a WARN in the log.
 */
@Slf4j
@Component
public class GoogleBooksClient {

    private static final int MAX_RESULTS = 10;

    private final RestClient restClient;
    private final String apiKey;

    public GoogleBooksClient(RestClient.Builder builder, AppProperties properties) {
        AppProperties.GoogleBooks config = properties.googleBooks();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(config.timeout())
                .withReadTimeout(config.timeout());
        this.restClient = builder
                .baseUrl(config.baseUrl())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
        this.apiKey = config.apiKey();
    }

    public List<BookMetadata> searchByText(String query) {
        return fetch(query, true);
    }

    public Optional<BookMetadata> findByIsbn(String isbn13) {
        return fetch("isbn:" + isbn13, false).stream().findFirst();
    }

    private List<BookMetadata> fetch(String query, boolean russianOnly) {
        try {
            JsonNode body = restClient.get()
                    .uri(uri -> {
                        uri.path("/volumes")
                                .queryParam("q", query)
                                .queryParam("maxResults", MAX_RESULTS);
                        if (russianOnly) {
                            uri.queryParam("langRestrict", "ru");
                        }
                        if (StringUtils.hasText(apiKey)) {
                            uri.queryParam("key", apiKey);
                        }
                        return uri.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
            return body == null ? List.of() : toMetadata(body.path("items"));
        } catch (RestClientException e) {
            log.warn("Google Books request failed for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    private static List<BookMetadata> toMetadata(JsonNode items) {
        List<BookMetadata> result = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode info = item.path("volumeInfo");
            String title = text(info, "title");
            if (title == null) {
                continue;
            }
            result.add(new BookMetadata(
                    title,
                    authors(info.path("authors")),
                    text(info, "description"),
                    isbn(info.path("industryIdentifiers")),
                    text(info, "publisher"),
                    year(text(info, "publishedDate")),
                    info.path("pageCount").isInt() ? info.path("pageCount").asInt() : null,
                    language(text(info, "language")),
                    cover(text(info.path("imageLinks"), "thumbnail"))
            ));
        }
        return result;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }

    private static String authors(JsonNode authors) {
        List<String> names = new ArrayList<>();
        authors.forEach(name -> names.add(name.asText()));
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private static String isbn(JsonNode identifiers) {
        // Prefer ISBN-13, fall back to ISBN-10 converted to 13 digits
        for (String type : List.of("ISBN_13", "ISBN_10")) {
            for (JsonNode id : identifiers) {
                if (type.equals(id.path("type").asText())) {
                    String isbn13 = IsbnUtils.normalize(id.path("identifier").asText());
                    if (isbn13 != null) {
                        return isbn13;
                    }
                }
            }
        }
        return null;
    }

    private static Integer year(String publishedDate) {
        // "2023", "2023-05" or "2023-05-17"
        if (publishedDate == null || !publishedDate.matches("\\d{4}.*")) {
            return null;
        }
        return Integer.valueOf(publishedDate.substring(0, 4));
    }

    private static String language(String language) {
        return language != null && language.length() <= 5 ? language : null;
    }

    private static String cover(String url) {
        // Google returns http:// thumbnails; https avoids mixed content on the site
        return url == null ? null : url.replaceFirst("^http://", "https://");
    }
}
