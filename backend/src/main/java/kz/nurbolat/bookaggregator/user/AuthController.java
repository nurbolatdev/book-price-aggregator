package kz.nurbolat.bookaggregator.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.auth.RefreshCookies;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.auth.EmailVerificationService;
import kz.nurbolat.bookaggregator.auth.PasswordResetService;
import kz.nurbolat.bookaggregator.user.dto.AuthResponse;
import kz.nurbolat.bookaggregator.user.dto.EmailRequest;
import kz.nurbolat.bookaggregator.user.dto.LoginRequest;
import kz.nurbolat.bookaggregator.user.dto.RegisterRequest;
import kz.nurbolat.bookaggregator.user.dto.RegisterResponse;
import kz.nurbolat.bookaggregator.user.dto.ResetPasswordRequest;
import kz.nurbolat.bookaggregator.user.dto.TokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import kz.nurbolat.bookaggregator.config.OpenApiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@Tag(name = "Auth", description = "Регистрация, вход, обновление токенов, подтверждение email и сброс пароля")
@SecurityRequirements
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final RefreshCookies refreshCookies;

    @Operation(summary = "Регистрация", description = "Создаёт аккаунт и отправляет письмо для подтверждения email.")
    @ApiResponse(responseCode = "201", description = "Аккаунт создан")
    @ApiResponse(responseCode = "409", description = "Email уже зарегистрирован")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @PostMapping("/register")
    ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Вход", description = "Возвращает access-токен и данные пользователя; refresh-токен — в cookie kr_refresh.")
    @ApiResponse(responseCode = "401", description = "Неверный email или пароль")
    @ApiResponse(responseCode = "403", description = "Email не подтверждён")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                       @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return withCookie(authService.login(request, userAgent));
    }

    @Operation(summary = "Обновление пары токенов",
            description = "Меняет refresh-токен на новую пару; использованный токен перестаёт действовать. "
                    + "Токен берётся только из cookie kr_refresh, тело запроса игнорируется. "
                    + "Новый refresh-токен приходит только в cookie, в теле — access-токен и пользователь. "
                    + "Запрос обязан прийти с разрешённым заголовком Origin.")
    @ApiResponse(responseCode = "401", description = "Токена нет, сессия недействительна или истекла")
    @ApiResponse(responseCode = "403", description = "Origin не разрешён или не указан")
    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
                                         @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        String token = refreshCookies.read(request)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Сессия недействительна, войдите снова"));
        refreshCookies.checkOrigin(request, true);
        return withCookie(authService.refresh(token, userAgent));
    }

    @Operation(summary = "Выход",
            description = "Завершает сессию, к которой относится refresh-токен из cookie kr_refresh, "
                    + "и удаляет cookie. Без cookie тоже отвечает 204. Cookie принимается только с разрешённым заголовком Origin.")
    @ApiResponse(responseCode = "204", description = "Сессия завершена")
    @ApiResponse(responseCode = "403", description = "Origin не разрешён или не указан при токене из cookie")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        Optional<String> fromCookie = refreshCookies.read(request);
        refreshCookies.checkOrigin(request, fromCookie.isPresent());
        fromCookie.ifPresent(authService::logout);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookies.clear()).build();
    }

    @Operation(summary = "Выход на всех устройствах", description = "Завершает все сессии пользователя, включая текущую.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponse(responseCode = "204", description = "Все сессии завершены")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutAll() {
        accountService.logoutAll();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookies.clear()).build();
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
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookies.clear()).build();
    }

    private ResponseEntity<AuthResponse> withCookie(AuthService.SignedIn signedIn) {
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookies.issue(signedIn.refreshToken()))
                .body(signedIn.response());
    }
}
