package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @Schema(description = "Refresh-токен, полученный при входе. Не нужен, если браузер шлёт cookie kr_refresh; "
                + "поле нужно только до перехода frontend на cookie (F4)",
                example = "3f1c0c1e5a7b4f4a9d52c7e1b6a8d2f0", deprecated = true)
        @Size(max = 100, message = "Не больше 100 символов") String refreshToken) {
}
