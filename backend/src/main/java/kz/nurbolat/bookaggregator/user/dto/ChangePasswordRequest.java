package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Schema(description = "Текущий пароль", example = "secret123")
        @NotBlank(message = "Обязательное поле") String currentPassword,
        @Schema(description = "Новый пароль, от 8 до 100 символов", example = "newSecret123")
        @NotBlank(message = "Обязательное поле")
        @Size(min = 8, message = "Минимум 8 символов")
        @Size(max = 100, message = "Не больше 100 символов") String newPassword
) {
}
