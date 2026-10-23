package kz.nurbolat.bookaggregator.auth;

import com.jayway.jsonpath.JsonPath;
import jakarta.mail.internet.MimeMessage;
import kz.nurbolat.bookaggregator.support.AbstractMailIT;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetIT extends AbstractMailIT {

    private static final String OLD_PASSWORD = "old-secret1";
    private static final String NEW_PASSWORD = "new-secret2";
    private static final String RESET_LINK = "http://localhost:3000/reset-password";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailTokenService emailTokenService;

    @Test
    void shouldSetNewPasswordAndSignOutEverywhereWhenResetLinkIsUsed() throws Exception {
        String email = registered();
        String refreshToken = JsonPath.read(login(email, OLD_PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.refreshToken");

        postJson("/api/v1/auth/forgot-password", "{\"email\": \"" + email + "\"}").andExpect(status().isNoContent());
        List<MimeMessage> mail = awaitMail(email, 2);
        MimeMessage reset = mail.stream().filter(this::isReset).findFirst().orElseThrow();
        String token = linkToken(reset, RESET_LINK);

        postJson("/api/v1/auth/reset-password", resetJson(token)).andExpect(status().isNoContent());

        login(email, OLD_PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login(email, NEW_PASSWORD).andExpect(status().isOk());
        postJson("/api/v1/auth/refresh", "{\"refreshToken\": \"" + refreshToken + "\"}")
                .andExpect(status().isUnauthorized());
        // The link works once
        postJson("/api/v1/auth/reset-password", resetJson(token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void shouldAnswer204AndSendNothingWhenEmailIsUnknown() throws Exception {
        String email = "nobody-" + UUID.randomUUID() + "@example.com";

        postJson("/api/v1/auth/forgot-password", "{\"email\": \"" + email + "\"}").andExpect(status().isNoContent());

        Thread.sleep(1000);
        assertThat(inbox(email)).isEmpty();
    }

    @Test
    void shouldRejectResetWhenLinkExpiredOrIsForEmailConfirmation() throws Exception {
        User user = userRepository.findByEmail(registered()).orElseThrow();
        String expired = emailTokenService.issue(user, EmailTokenPurpose.RESET_PASSWORD, Duration.ofSeconds(-1));
        String verifyToken = emailTokenService.issue(user, EmailTokenPurpose.VERIFY_EMAIL, Duration.ofHours(1));

        postJson("/api/v1/auth/reset-password", resetJson(expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        postJson("/api/v1/auth/reset-password", resetJson(verifyToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    private boolean isReset(MimeMessage message) {
        try {
            return message.getSubject().startsWith("Сброс пароля");
        } catch (jakarta.mail.MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String registered() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        postJson("/api/v1/auth/register", "{\"email\": \"" + email + "\", \"password\": \"" + OLD_PASSWORD + "\"}")
                .andExpect(status().isCreated());
        return email;
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return postJson("/api/v1/auth/login", "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}");
    }

    private static String resetJson(String token) {
        return "{\"token\": \"" + token + "\", \"newPassword\": \"" + NEW_PASSWORD + "\"}";
    }
}
