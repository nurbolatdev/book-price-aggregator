package kz.nurbolat.bookaggregator.user;

import kz.nurbolat.bookaggregator.admin.SystemEventService;
import kz.nurbolat.bookaggregator.auth.RefreshTokenService;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.common.error.ErrorResponse.FieldError;
import kz.nurbolat.bookaggregator.security.AuthenticatedUser;
import kz.nurbolat.bookaggregator.security.SecurityUtils;
import kz.nurbolat.bookaggregator.user.dto.ChangePasswordRequest;
import kz.nurbolat.bookaggregator.user.dto.DeleteAccountRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Account actions of the signed-in user: password change, sign-out everywhere and account deletion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String WRONG_PASSWORD = "Неверный пароль";

    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final SystemEventService systemEventService;

    /**
     * Sets the new password and ends the user's other sessions. The current session is the one of the access token
     * ({@code sid}); old clients may still send its refresh token instead. Without either every session ends.
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = securityUtils.getCurrentUser();
        requirePassword(user, request.currentPassword(), "currentPassword");
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        UUID keptFamily = securityUtils.currentUser().map(AuthenticatedUser::sessionId).orElse(null);
        if (keptFamily == null && request.refreshToken() != null) {
            keptFamily = refreshTokenService.activeFamilyOf(request.refreshToken(), user.getId()).orElse(null);
        }
        if (keptFamily == null) {
            refreshTokenService.revokeAll(user.getId());
        } else {
            refreshTokenService.revokeAllExcept(user.getId(), keptFamily);
        }
        log.info("Password changed: user={}", user.getId());
        systemEventService.info("auth", "Пароль изменён", Map.of("userId", user.getId()));
    }

    /** Ends every session of the user, this device included. */
    @Transactional
    public void logoutAll() {
        User user = securityUtils.getCurrentUser();
        refreshTokenService.revokeAll(user.getId());
        log.info("All sessions signed out: user={}", user.getId());
    }

    /**
     * Deletes the account with everything it owns (the database cascades). The journal gets no email address.
     *
     * @throws AppException {@code CONFLICT} for an administrator, {@code VALIDATION_FAILED} for a wrong password
     */
    @Transactional
    public void delete(DeleteAccountRequest request) {
        User user = securityUtils.getCurrentUser();
        if (user.getRole() == User.Role.ADMIN) {
            throw new AppException(ErrorCode.CONFLICT, "Администратор не может удалить свой аккаунт");
        }
        requirePassword(user, request.password(), "password");
        Long id = user.getId();
        userRepository.deleteUserById(id);
        log.info("Account deleted: user={}", id);
        systemEventService.info("user", "Аккаунт удалён", Map.of("event", "USER_DELETED", "userId", id));
    }

    private void requirePassword(User user, String password, String field) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                    List.of(new FieldError(field, WRONG_PASSWORD)));
        }
    }
}
