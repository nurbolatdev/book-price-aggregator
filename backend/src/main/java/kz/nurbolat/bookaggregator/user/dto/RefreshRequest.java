package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @Schema(description = "Refresh-токен, полученный при входе", example = "3f1c0c1e5a7b4f4a9d52c7e1b6a8d2f0") @NotBlank @Size(max = 100) String refreshToken) {
}
