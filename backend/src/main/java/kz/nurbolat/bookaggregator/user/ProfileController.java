package kz.nurbolat.bookaggregator.user;

import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.user.dto.ChangePasswordRequest;
import kz.nurbolat.bookaggregator.user.dto.DeleteAccountRequest;
import kz.nurbolat.bookaggregator.user.dto.UpdateProfileRequest;
import kz.nurbolat.bookaggregator.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Profile", description = "Профиль текущего пользователя")
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final AccountService accountService;

    @Operation(summary = "Профиль", description = "Данные текущего пользователя.")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @GetMapping
    UserResponse get() {
        return profileService.get();
    }

    @Operation(summary = "Смена пароля",
            description = "Проверяет текущий пароль и ставит новый. Сессия переданного refresh-токена остаётся, "
                    + "остальные завершаются; без токена завершаются все.")
    @ApiResponse(responseCode = "204", description = "Пароль изменён")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @ApiResponse(responseCode = "422", description = "Неверный текущий пароль или новый пароль не подходит")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @PostMapping("/password")
    ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Удаление аккаунта",
            description = "Удаляет аккаунт со всеми маячками, письмами и сессиями после проверки пароля. "
                    + "Администратор удалить аккаунт не может.")
    @ApiResponse(responseCode = "204", description = "Аккаунт удалён")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @ApiResponse(responseCode = "409", description = "Аккаунт администратора удалить нельзя")
    @ApiResponse(responseCode = "422", description = "Неверный пароль")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @DeleteMapping
    ResponseEntity<Void> delete(@Valid @RequestBody DeleteAccountRequest request) {
        accountService.delete(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Изменение профиля", description = "Меняет только переданные поля.")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @PatchMapping
    UserResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return profileService.update(request);
    }
}
