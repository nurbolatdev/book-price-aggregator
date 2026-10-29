package kz.nurbolat.bookaggregator.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * @param editionId edition the offer belongs to
 */
public record OfferMatchRequest(@Schema(description = "Идентификатор издания", example = "42") @NotNull(message = "Обязательное поле") Long editionId) {
}
