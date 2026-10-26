package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Token from a link in an email.
 */
public record TokenRequest(
        @Schema(description = "Токен из ссылки в письме", example = "3f1c0c1e5a7b4f4a9d52c7e1b6a8d2f0") @NotBlank @Size(max = 100) String token) {
}
