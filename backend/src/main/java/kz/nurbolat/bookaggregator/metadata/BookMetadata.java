package kz.nurbolat.bookaggregator.metadata;

/**
 * Bibliographic data of one book edition from an external catalog. Never contains prices.
 *
 * @param isbn13 normalized 13-digit ISBN, or {@code null} if the source has no valid ISBN
 */
public record BookMetadata(
        String title,
        String author,
        String description,
        String isbn13,
        String publisher,
        Integer year,
        Integer pages,
        String language,
        String coverUrl
) {
}
