package kz.nurbolat.bookaggregator.metadata;

import kz.nurbolat.bookaggregator.edition.Binding;

/**
 * Bibliographic data of one book edition from an external catalog or a store page.
 * Never contains prices.
 *
 * @param isbn13  normalized 13-digit ISBN, or {@code null} if the source has no valid ISBN
 * @param binding {@code null} when unknown (Google Books does not report it)
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
        String coverUrl,
        Binding binding
) {
}
