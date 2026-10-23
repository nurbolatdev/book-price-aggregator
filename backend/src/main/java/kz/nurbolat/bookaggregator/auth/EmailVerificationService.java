package kz.nurbolat.bookaggregator.auth;

import kz.nurbolat.bookaggregator.common.text.Emails;
import kz.nurbolat.bookaggregator.config.AppProperties;
import kz.nurbolat.bookaggregator.notification.EmailPublisher;
import kz.nurbolat.bookaggregator.notification.EmailType;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/**
 * Confirmation of the email address: a link with a one-time token, valid for 24 hours.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailTokenService emailTokenService;
    private final EmailPublisher emailPublisher;
    private final UserRepository userRepository;
    private final AuthProperties properties;
    private final AppProperties appProperties;
    private final Clock clock;

    /** Sends the confirmation link; the email leaves after the surrounding transaction commits. */
    @Transactional
    public void sendLink(User user) {
        String token = emailTokenService.issue(user, EmailTokenPurpose.VERIFY_EMAIL, properties.verifyEmailTtl());
        Map<String, Object> model = new HashMap<>();
        model.put("name", user.getName());
        model.put("link", appProperties.baseUrl() + "/verify-email?token=" + token);
        emailPublisher.publish(EmailType.VERIFY_EMAIL, user.getEmail(), model);
    }

    /**
     * @throws kz.nurbolat.bookaggregator.common.error.AppException {@code TOKEN_INVALID} or {@code TOKEN_EXPIRED}
     */
    @Transactional
    public void verify(String token) {
        User user = emailTokenService.consume(token, EmailTokenPurpose.VERIFY_EMAIL);
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            log.info("Email confirmed: user={}", user.getId());
        }
    }

    /**
     * Sends a new link, at most once a minute per user. Says nothing about whether the address is registered:
     * an unknown or already confirmed address is silently ignored.
     */
    @Transactional
    public void resend(String email) {
        userRepository.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> {
                    if (emailTokenService.issuedSince(user, EmailTokenPurpose.VERIFY_EMAIL,
                            clock.instant().minus(properties.resendInterval()))) {
                        log.info("Confirmation email to {} not resent: the last one is less than {} old",
                                Emails.mask(email), properties.resendInterval());
                        return;
                    }
                    sendLink(user);
                });
    }
}
