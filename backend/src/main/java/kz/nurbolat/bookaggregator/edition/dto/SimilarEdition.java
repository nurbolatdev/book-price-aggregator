package kz.nurbolat.bookaggregator.edition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Похожая книга: показывается издание с самой низкой ценой")
public record SimilarEdition(
        Long bookId,
        Long editionId,
        String title,
        String author,
        String coverUrl,
        @Schema(description = "Минимальная цена в наличии, ₸") BigDecimal bestPrice) {
}
