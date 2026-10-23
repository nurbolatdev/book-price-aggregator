package kz.nurbolat.bookaggregator.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kz.nurbolat.bookaggregator.user.NotifyFrequency;

/**
 * Fields left out ({@code null}) stay as they are.
 */
public record UpdateProfileRequest(
        @Size(max = 100) @Pattern(regexp = ".*\\S.*", message = "не должно быть пустым") String name,
        NotifyFrequency notifyFrequency,
        Boolean weeklyDigest
) {
}
