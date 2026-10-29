package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @Schema(description = "Текущий пароль для подтверждения", example = "secret123")
        @NotBlank String password
) {
}
