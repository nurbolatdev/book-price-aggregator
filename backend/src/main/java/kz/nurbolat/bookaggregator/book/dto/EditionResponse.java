package kz.nurbolat.bookaggregator.book.dto;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.edition.Edition;

public record EditionResponse(
        Long editionId,
        String isbn13,
        String publisher,
        Short year,
        Binding binding,
        String language,
        Integer pages,
        String coverUrl
) {

    public static EditionResponse from(Edition edition) {
        return new EditionResponse(
                edition.getId(),
                edition.getIsbn13(),
                edition.getPublisher(),
                edition.getYear(),
                edition.getBinding(),
                edition.getLanguage(),
                edition.getPages(),
                edition.getCoverUrl()
        );
    }
}
