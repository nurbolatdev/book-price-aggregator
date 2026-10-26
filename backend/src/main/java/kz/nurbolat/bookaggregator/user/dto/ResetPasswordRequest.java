package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @Schema(description = "Токен из письма для сброса пароля", example = "3f1c0c1e5a7b4f4a9d52c7e1b6a8d2f0")
        @NotBlank @Size(max = 100) String token,
        @Schema(description = "Новый пароль, не короче 6 символов", example = "newSecret123")
        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String newPassword
) {
}
