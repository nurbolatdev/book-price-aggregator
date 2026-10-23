package kz.nurbolat.bookaggregator.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Token from a link in an email.
 */
public record TokenRequest(@NotBlank @Size(max = 100) String token) {
}
