package kz.nurbolat.bookaggregator.book.dto;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.edition.Edition;

import java.util.List;
import java.util.Objects;

/**
 * Book with its editions. Prices come back on the edition level in M14.
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
     * Must be called inside a transaction: reads the lazy editions collection.
     */
    public static BookResponse from(Book book) {
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
                editions.stream().map(EditionResponse::from).toList()
        );
    }
}
