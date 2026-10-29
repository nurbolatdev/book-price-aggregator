package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Schema(description = "Текущий пароль", example = "secret123")
        @NotBlank String currentPassword,
        @Schema(description = "Новый пароль, от 8 до 100 символов", example = "newSecret123")
        @NotBlank
        @Size(min = 8, message = "Минимум 8 символов")
        @Size(max = 100, message = "Не больше 100 символов") String newPassword,
        @Schema(description = "Refresh-токен этого устройства: его сессия остаётся, остальные завершаются. "
                + "Без него завершаются все сессии", example = "3f1c0c1e5a7b4f4a9d52c7e1b6a8d2f0")
        @Size(max = 100) String refreshToken
) {
}
