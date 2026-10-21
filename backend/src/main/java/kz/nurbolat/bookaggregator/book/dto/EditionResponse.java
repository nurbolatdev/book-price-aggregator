package kz.nurbolat.bookaggregator.book.dto;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.edition.Edition;

import java.math.BigDecimal;

/**
 * @param bestPrice lowest in-stock price of the edition, {@code null} when not on sale
 */
public record EditionResponse(
        Long editionId,
        String isbn13,
        String publisher,
        Short year,
        Binding binding,
        String language,
        Integer pages,
        String coverUrl,
        BigDecimal bestPrice
) {

    public static EditionResponse from(Edition edition, BigDecimal bestPrice) {
        return new EditionResponse(
                edition.getId(),
                edition.getIsbn13(),
                edition.getPublisher(),
                edition.getYear(),
                edition.getBinding(),
                edition.getLanguage(),
                edition.getPages(),
                edition.getCoverUrl(),
                bestPrice
        );
    }
}
