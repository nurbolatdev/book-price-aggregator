package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Schema(description = "Токен из письма для сброса пароля", example = "3f1c0c1e5a7b4f4a9d52c7e1b6a8d2f0")
        @NotBlank(message = "Обязательное поле") @Size(max = 100, message = "Не больше 100 символов") String token,
        @Schema(description = "Новый пароль, 8–100 символов", example = "newSecret123")
        @NotBlank(message = "Обязательное поле")
        @Size(min = 8, message = "Минимум 8 символов")
        @Size(max = 100, message = "Не больше 100 символов") String newPassword
) {
}
