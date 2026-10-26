package kz.nurbolat.bookaggregator.user;

import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.user.dto.UpdateProfileRequest;
import kz.nurbolat.bookaggregator.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Profile", description = "Профиль текущего пользователя")
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "Профиль", description = "Данные текущего пользователя.")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @GetMapping
    UserResponse get() {
        return profileService.get();
    }

    @Operation(summary = "Изменение профиля", description = "Меняет только переданные поля.")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @PatchMapping
    UserResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(request);
    }
}
