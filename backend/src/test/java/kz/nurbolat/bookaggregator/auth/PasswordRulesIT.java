package kz.nurbolat.bookaggregator.auth;

import com.jayway.jsonpath.JsonPath;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordRulesIT extends AbstractIntegrationTest {

    private static final String SHORT = "1234567";
    private static final String TOO_LONG = "a".repeat(101);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldRejectShortPasswordOnRegister() throws Exception {
        register(email(), SHORT)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Минимум 8 символов"));
    }

    @Test
    void shouldRejectShortPasswordOnReset() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"abc\", \"newPassword\": \"" + SHORT + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Минимум 8 символов"));
    }

    @Test
    void shouldRejectShortPasswordOnChange() throws Exception {
        String bearer = signedIn();
        changePassword(bearer, SHORT)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Минимум 8 символов"));
    }

    @Test
    void shouldRejectTooLongPassword() throws Exception {
        register(email(), TOO_LONG)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Не больше 100 символов"));
        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"abc\", \"newPassword\": \"" + TOO_LONG + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Не больше 100 символов"));
        changePassword(signedIn(), TOO_LONG)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Не больше 100 символов"));
    }

    @Test
    void shouldSignInWithLegacySixCharacterPassword() throws Exception {
        String email = email();
        jdbc.update("INSERT INTO users (email, password) VALUES (?, ?)", email, passwordEncoder.encode("abc123"));

        login(email, "abc123").andExpect(status().isOk());
    }

    @Test
    void shouldKeepRussianMessagesWhenClientAsksForEnglish() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"not-an-email\", \"password\": \"" + SHORT + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='password')].message").value("Минимум 8 символов"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='email')].message").value("Некорректный email"));
    }

    private ResultActions register(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
    }

    private ResultActions changePassword(String bearer, String next) throws Exception {
        return mockMvc.perform(post("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\": \"secret123\", \"newPassword\": \"" + next + "\"}"));
    }

    private String signedIn() throws Exception {
        String email = email();
        register(email, "secret123").andExpect(status().isCreated());
        String body = login(email, "secret123").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.<String>read(body, "$.accessToken");
    }

    private static String email() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
