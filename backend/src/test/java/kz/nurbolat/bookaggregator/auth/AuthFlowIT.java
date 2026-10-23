package kz.nurbolat.bookaggregator.auth;

import com.jayway.jsonpath.JsonPath;
import jakarta.mail.internet.MimeMessage;
import kz.nurbolat.bookaggregator.support.AbstractMailIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIT extends AbstractMailIT {

    private static final String PASSWORD = "secret123";
    private static final String VERIFY_LINK = "http://localhost:3000/verify-email";

    @Test
    void shouldConfirmEmailWhenLinkFromRegistrationEmailIsUsed() throws Exception {
        String email = uniqueEmail();

        postJson("/api/v1/auth/register", registerJson(email))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailVerificationRequired").value(true));
        login(email).andExpect(jsonPath("$.user.emailVerified").value(false));

        List<MimeMessage> mail = awaitMail(email, 1);
        assertThat(mail).hasSize(1);
        assertThat(mail.get(0).getSubject()).isEqualTo("Подтвердите email — KitapRadar");
        String token = linkToken(mail.get(0), VERIFY_LINK);

        postJson("/api/v1/auth/verify-email", "{\"token\": \"" + token + "\"}")
                .andExpect(status().isNoContent());

        String body = login(email)
                .andExpect(jsonPath("$.user.emailVerified").value(true))
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(body, "$.accessToken");
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void shouldRejectLinkWhenItWasAlreadyUsedOrIsUnknown() throws Exception {
        String email = uniqueEmail();
        postJson("/api/v1/auth/register", registerJson(email)).andExpect(status().isCreated());
        String token = linkToken(awaitMail(email, 1).get(0), VERIFY_LINK);
        postJson("/api/v1/auth/verify-email", "{\"token\": \"" + token + "\"}").andExpect(status().isNoContent());

        postJson("/api/v1/auth/verify-email", "{\"token\": \"" + token + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
        postJson("/api/v1/auth/verify-email", "{\"token\": \"not-a-real-token\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void shouldNotSendSecondEmailWhenResendIsAskedWithinAMinute() throws Exception {
        String email = uniqueEmail();
        postJson("/api/v1/auth/register", registerJson(email)).andExpect(status().isCreated());
        awaitMail(email, 1);

        postJson("/api/v1/auth/resend-verification", "{\"email\": \"" + email + "\"}")
                .andExpect(status().isNoContent());

        Thread.sleep(1500);
        assertThat(inbox(email)).hasSize(1);
    }

    @Test
    void shouldAnswer204AndSendNothingWhenResendIsForUnknownEmail() throws Exception {
        String email = uniqueEmail();

        postJson("/api/v1/auth/resend-verification", "{\"email\": \"" + email + "\"}")
                .andExpect(status().isNoContent());

        Thread.sleep(1000);
        assertThat(inbox(email)).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions login(String email) throws Exception {
        return postJson("/api/v1/auth/login", "{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}")
                .andExpect(status().isOk());
    }

    private static String registerJson(String email) {
        return "{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\", \"name\": \"Тест\"}";
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
