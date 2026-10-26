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
    @NotBlank
    @Email
    private String email;

    @Schema(description = "Пароль, не короче 6 символов", example = "secret123")
    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Schema(description = "Отображаемое имя, необязательно", example = "Айгерим")
    private String name;
}
