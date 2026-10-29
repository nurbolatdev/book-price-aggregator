package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @Schema(description = "Email пользователя", example = "reader@example.com") @NotBlank(message = "Обязательное поле") @Email(message = "Некорректный email") String email) {
}
