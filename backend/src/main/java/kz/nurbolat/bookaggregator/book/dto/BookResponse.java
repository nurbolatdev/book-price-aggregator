package kz.nurbolat.bookaggregator.book.dto;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.edition.Edition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Book with its editions and their best prices.
 *
 * @param coverUrl cover of the first edition that has one, for list views
 */
public record BookResponse(
        Long id,
        String title,
        String author,
        String description,
        String coverUrl,
        List<EditionResponse> editions
) {

    /**
     * Without prices. Must be called inside a transaction: reads the lazy editions collection.
     */
    public static BookResponse from(Book book) {
        return from(book, Map.of());
    }

    /**
     * @param bestPrices lowest in-stock price by edition id; editions not on sale are absent
     */
    public static BookResponse from(Book book, Map<Long, BigDecimal> bestPrices) {
        List<Edition> editions = book.getEditions();
        String coverUrl = editions.stream()
                .map(Edition::getCoverUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                coverUrl,
                editions.stream()
                        .map(edition -> EditionResponse.from(edition, bestPrices.get(edition.getId())))
                        .toList()
        );
    }
}
