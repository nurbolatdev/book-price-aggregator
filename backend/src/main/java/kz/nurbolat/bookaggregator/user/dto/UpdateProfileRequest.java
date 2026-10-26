package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kz.nurbolat.bookaggregator.user.NotifyFrequency;

/**
 * Fields left out ({@code null}) stay as they are.
 */
public record UpdateProfileRequest(
        @Schema(description = "Новое имя; не пустое", example = "Айгерим")
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "не должно быть пустым") String name,
        @Schema(description = "Как часто присылать письма о ценах") NotifyFrequency notifyFrequency,
        @Schema(description = "Присылать еженедельную сводку снижений цен") Boolean weeklyDigest
) {
}
