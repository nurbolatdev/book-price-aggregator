package kz.nurbolat.bookaggregator.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * @param editionId edition the offer belongs to
 */
public record OfferMatchRequest(@NotNull Long editionId) {
}
