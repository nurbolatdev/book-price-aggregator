package kz.nurbolat.bookaggregator.source;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleBooksAdapter implements BookSource {

    private final RestTemplate restTemplate;

    @Value("${sources.google-books.api-key:}")
    private String apiKey;

    @Value("${sources.google-books.base-url:https://www.googleapis.com/books/v1}")
    private String baseUrl;

    @Override
    public String getName() {
        return "google_books";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BookSourceResult> search(String query) {
        List<BookSourceResult> results = new ArrayList<>();

        try {
            UriComponentsBuilder uri = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/volumes")
                    .queryParam("q", query)
                    .queryParam("maxResults", 10)
                    .queryParam("langRestrict", "ru");

            if (!apiKey.isBlank()) {
                uri.queryParam("key", apiKey);
            }

            Map<String, Object> response = restTemplate.getForObject(uri.toUriString(), Map.class);
            if (response == null || !response.containsKey("items")) {
                return results;
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

            for (Map<String, Object> item : items) {
                try {
                    results.add(mapToResult(item));
                } catch (Exception e) {
                    log.warn("Failed to map Google Books item: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Google Books search failed for query '{}': {}", query, e.getMessage());
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private BookSourceResult mapToResult(Map<String, Object> item) {
        Map<String, Object> info = (Map<String, Object>) item.get("volumeInfo");

        String title = (String) info.getOrDefault("title", "Unknown");
        List<String> authors = (List<String>) info.getOrDefault("authors", List.of());
        String author = authors.isEmpty() ? null : String.join(", ", authors);
        String publisher = (String) info.get("publisher");
        String description = (String) info.get("description");

        String isbn = extractIsbn((List<Map<String, String>>) info.getOrDefault("industryIdentifiers", List.of()));

        Map<String, Object> imageLinks = (Map<String, Object>) info.getOrDefault("imageLinks", Map.of());
        String coverUrl = (String) imageLinks.get("thumbnail");

        return BookSourceResult.builder()
                .title(title)
                .author(author)
                .isbn(isbn)
                .publisher(publisher)
                .description(description)
                .coverUrl(coverUrl)
                .year(extractYear((String) info.get("publishedDate")))
                .pages(info.get("pageCount") instanceof Number count ? count.intValue() : null)
                .language((String) info.get("language"))
                .source(getName())
                .build();
    }

    private static String extractIsbn(List<Map<String, String>> identifiers) {
        // Prefer ISBN-13, fall back to ISBN-10 (converted to 13 digits when the edition is saved)
        for (String type : List.of("ISBN_13", "ISBN_10")) {
            for (Map<String, String> id : identifiers) {
                if (type.equals(id.get("type"))) {
                    return id.get("identifier");
                }
            }
        }
        return null;
    }

    private static Integer extractYear(String publishedDate) {
        // Google returns "2023", "2023-05" or "2023-05-17"
        if (publishedDate == null || !publishedDate.matches("\\d{4}.*")) {
            return null;
        }
        return Integer.valueOf(publishedDate.substring(0, 4));
    }
}
