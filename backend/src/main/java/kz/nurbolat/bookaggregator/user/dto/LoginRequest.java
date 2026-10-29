package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @Schema(description = "Email", example = "reader@example.com")
    @NotBlank(message = "Обязательное поле")
    @Email(message = "Некорректный email")
    private String email;

    @Schema(description = "Пароль", example = "secret123")
    @NotBlank(message = "Обязательное поле")
    private String password;
}
