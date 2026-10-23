package kz.nurbolat.bookaggregator.auth;

import kz.nurbolat.bookaggregator.common.text.Emails;
import kz.nurbolat.bookaggregator.config.AppProperties;
import kz.nurbolat.bookaggregator.notification.EmailPublisher;
import kz.nurbolat.bookaggregator.notification.EmailType;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Password reset by a one-time link valid for an hour. Setting the new password signs the user out everywhere.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailTokenService emailTokenService;
    private final EmailPublisher emailPublisher;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;
    private final AppProperties appProperties;

    /**
     * Emails a reset link if the address is registered; an unknown address is silently ignored, so that the
     * answer does not tell who has an account.
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            String token = emailTokenService.issue(user, EmailTokenPurpose.RESET_PASSWORD, properties.resetPasswordTtl());
            Map<String, Object> model = new HashMap<>();
            model.put("name", user.getName());
            model.put("link", appProperties.baseUrl() + "/reset-password?token=" + token);
            emailPublisher.publish(EmailType.RESET_PASSWORD, user.getEmail(), model);
        }, () -> log.info("Password reset for unknown email {} ignored", Emails.mask(email)));
    }

    /**
     * @throws kz.nurbolat.bookaggregator.common.error.AppException {@code TOKEN_INVALID} or {@code TOKEN_EXPIRED}
     */
    @Transactional
    public void reset(String token, String newPassword) {
        User user = emailTokenService.consume(token, EmailTokenPurpose.RESET_PASSWORD);
        user.setPassword(passwordEncoder.encode(newPassword));
        refreshTokenService.revokeAll(user.getId());
        log.info("Password reset: user={}, all sessions signed out", user.getId());
    }
}
