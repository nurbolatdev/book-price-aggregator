package kz.nurbolat.bookaggregator.user;

import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.auth.EmailVerificationService;
import kz.nurbolat.bookaggregator.auth.PasswordResetService;
import kz.nurbolat.bookaggregator.user.dto.AuthResponse;
import kz.nurbolat.bookaggregator.user.dto.EmailRequest;
import kz.nurbolat.bookaggregator.user.dto.LoginRequest;
import kz.nurbolat.bookaggregator.user.dto.RefreshRequest;
import kz.nurbolat.bookaggregator.user.dto.RegisterRequest;
import kz.nurbolat.bookaggregator.user.dto.RegisterResponse;
import kz.nurbolat.bookaggregator.user.dto.ResetPasswordRequest;
import kz.nurbolat.bookaggregator.user.dto.TokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth", description = "Регистрация, вход, обновление токенов, подтверждение email и сброс пароля")
@SecurityRequirements
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Регистрация", description = "Создаёт аккаунт и отправляет письмо для подтверждения email.")
    @ApiResponse(responseCode = "201", description = "Аккаунт создан")
    @ApiResponse(responseCode = "409", description = "Email уже зарегистрирован")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @PostMapping("/register")
    ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Вход", description = "Возвращает пару access и refresh токенов.")
    @ApiResponse(responseCode = "401", description = "Неверный email или пароль")
    @ApiResponse(responseCode = "403", description = "Email не подтверждён")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                       @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return ResponseEntity.ok(authService.login(request, userAgent));
    }

    @Operation(summary = "Обновление пары токенов", description = "Меняет refresh-токен на новую пару; использованный токен перестаёт действовать.")
    @ApiResponse(responseCode = "401", description = "Сессия недействительна или истекла")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                         @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken(), userAgent));
    }

    @Operation(summary = "Выход", description = "Завершает сессию, к которой относится refresh-токен.")
    @ApiResponse(responseCode = "204", description = "Сессия завершена")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Подтверждение email", description = "Подтверждает адрес по токену из письма.")
    @ApiResponse(responseCode = "204", description = "Email подтверждён")
    @ApiResponse(responseCode = "400", description = "Токен недействителен")
    @ApiResponse(responseCode = "401", description = "Срок действия токена истёк")
    @PostMapping("/verify-email")
    ResponseEntity<Void> verifyEmail(@Valid @RequestBody TokenRequest request) {
        emailVerificationService.verify(request.token());
        return ResponseEntity.noContent().build();
    }

    /** Always 204: the answer must not tell whether the email is registered. */
    @Operation(summary = "Повторная отправка письма подтверждения", description = "Всегда отвечает 204, чтобы не раскрывать, зарегистрирован ли адрес.")
    @ApiResponse(responseCode = "204", description = "Запрос принят")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @PostMapping("/resend-verification")
    ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailRequest request) {
        emailVerificationService.resend(request.email());
        return ResponseEntity.noContent().build();
    }

    /** Always 204: the answer must not tell whether the email is registered. */
    @Operation(summary = "Запрос сброса пароля", description = "Всегда отвечает 204, чтобы не раскрывать, зарегистрирован ли адрес.")
    @ApiResponse(responseCode = "204", description = "Запрос принят")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Сброс пароля", description = "Устанавливает новый пароль по токену из письма.")
    @ApiResponse(responseCode = "204", description = "Пароль изменён")
    @ApiResponse(responseCode = "400", description = "Токен недействителен")
    @ApiResponse(responseCode = "401", description = "Срок действия токена истёк")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @PostMapping("/reset-password")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
