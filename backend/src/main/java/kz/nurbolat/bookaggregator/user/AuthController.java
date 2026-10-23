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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                       @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return ResponseEntity.ok(authService.login(request, userAgent));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                         @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken(), userAgent));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    ResponseEntity<Void> verifyEmail(@Valid @RequestBody TokenRequest request) {
        emailVerificationService.verify(request.token());
        return ResponseEntity.noContent().build();
    }

    /** Always 204: the answer must not tell whether the email is registered. */
    @PostMapping("/resend-verification")
    ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailRequest request) {
        emailVerificationService.resend(request.email());
        return ResponseEntity.noContent().build();
    }

    /** Always 204: the answer must not tell whether the email is registered. */
    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
