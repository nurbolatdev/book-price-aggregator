package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @Schema(description = "Email; на него придёт письмо подтверждения", example = "reader@example.com")
    @NotBlank(message = "Обязательное поле")
    @Email(message = "Некорректный email")
    private String email;

    @Schema(description = "Пароль, 8–100 символов", example = "secret123")
    @NotBlank(message = "Обязательное поле")
    @Size(min = 8, message = "Минимум 8 символов")
    @Size(max = 100, message = "Не больше 100 символов")
    private String password;

    @Schema(description = "Отображаемое имя, необязательно", example = "Айгерим")
    private String name;
}
