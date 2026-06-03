package kz.nurbolat.bookaggregator.source;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
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
        Map<String, Object> saleInfo = (Map<String, Object>) item.getOrDefault("saleInfo", Map.of());

        String title = (String) info.getOrDefault("title", "Unknown");
        List<String> authors = (List<String>) info.getOrDefault("authors", List.of());
        String author = authors.isEmpty() ? null : String.join(", ", authors);
        String publisher = (String) info.get("publisher");
        String description = (String) info.get("description");

        List<String> industryIds = (List<Map<String, String>>) info.getOrDefault("industryIdentifiers", List.of())
                instanceof List<?> list ? list.stream()
                .filter(id -> id instanceof Map)
                .map(id -> (Map<String, String>) id)
                .filter(id -> "ISBN_13".equals(id.get("type")))
                .map(id -> id.get("identifier"))
                .toList() : List.of();
        String isbn = industryIds.isEmpty() ? null : industryIds.get(0);

        Map<String, Object> imageLinks = (Map<String, Object>) info.getOrDefault("imageLinks", Map.of());
        String coverUrl = (String) imageLinks.get("thumbnail");

        String buyLink = (String) saleInfo.getOrDefault("buyLink", "https://books.google.com");

        BigDecimal price = extractPrice(saleInfo);

        return BookSourceResult.builder()
                .title(title)
                .author(author)
                .isbn(isbn)
                .publisher(publisher)
                .description(description)
                .coverUrl(coverUrl)
                .source(getName())
                .price(price)
                .currency("KZT")
                .inStock(true)
                .url(buyLink)
                .build();
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractPrice(Map<String, Object> saleInfo) {
        try {
            Map<String, Object> listPrice = (Map<String, Object>) saleInfo.get("listPrice");
            if (listPrice != null && listPrice.containsKey("amount")) {
                double amount = ((Number) listPrice.get("amount")).doubleValue();
                return BigDecimal.valueOf(amount);
            }
        } catch (Exception ignored) {
        }
        return BigDecimal.valueOf(2500);
    }
}
