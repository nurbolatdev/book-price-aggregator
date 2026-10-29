package kz.nurbolat.bookaggregator.user;

import com.jayway.jsonpath.JsonPath;
import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileSecurityIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldKeepCurrentDeviceAndEndOthersWhenPasswordChanges() throws Exception {
        String email = registered();
        String deviceA = signIn(email);
        String deviceB = signIn(email);

        mockMvc.perform(post("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, bearerOf(deviceA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(PASSWORD, "newSecret123", refreshTokenOf(deviceA))))
                .andExpect(status().isNoContent());

        refresh(refreshTokenOf(deviceA)).andExpect(status().isOk());
        refresh(refreshTokenOf(deviceB)).andExpect(status().isUnauthorized());
        login(email, "newSecret123").andExpect(status().isOk());
        login(email, PASSWORD).andExpect(status().isUnauthorized());
        Integer events = jdbc.queryForObject(
                "SELECT count(*) FROM system_events WHERE source = 'auth' AND message = 'Пароль изменён'", Integer.class);
        assertThat(events).isPositive();
    }

    @Test
    void shouldEndEverySessionWhenPasswordChangesWithoutSessionInfo() throws Exception {
        String email = registered();
        String deviceA = signIn(email);
        // An access token issued before the session claim existed and no refresh token in the body
        String legacyBearer = "Bearer " + jwtService.generateAccessToken(userRepository.findByEmail(email).orElseThrow());

        mockMvc.perform(post("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, legacyBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(PASSWORD, "newSecret123", null)))
                .andExpect(status().isNoContent());

        refresh(refreshTokenOf(deviceA)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnFieldErrorWhenCurrentPasswordIsWrong() throws Exception {
        String session = signIn(registered());

        mockMvc.perform(post("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, bearerOf(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("wrong-password", "newSecret123", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Неверный пароль"));
    }

    @Test
    void shouldReturn422WhenNewPasswordIsTooShort() throws Exception {
        String session = signIn(registered());

        mockMvc.perform(post("/api/v1/me/password").header(HttpHeaders.AUTHORIZATION, bearerOf(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody(PASSWORD, "1234567", null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Минимум 8 символов"));
    }

    @Test
    void shouldEndEverySessionWhenLoggingOutEverywhere() throws Exception {
        String email = registered();
        String deviceA = signIn(email);
        String deviceB = signIn(email);

        mockMvc.perform(post("/api/v1/auth/logout-all").header(HttpHeaders.AUTHORIZATION, bearerOf(deviceA)))
                .andExpect(status().isNoContent());

        refresh(refreshTokenOf(deviceA)).andExpect(status().isUnauthorized());
        refresh(refreshTokenOf(deviceB)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenLoggingOutEverywhereWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout-all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldDeleteEverythingTheUserOwnsWhenAccountIsDeleted() throws Exception {
        String email = registered();
        String session = signIn(email);
        long userId = userId(email);
        long edition = editionWithHistory(jdbc, "Дюна", true, daysAgo(3, 5000));
        jdbc.update("INSERT INTO beacons (user_id, edition_id, condition_type) VALUES (?, ?, 'ANY_DROP')",
                userId, edition);
        jdbc.update("INSERT INTO notifications (user_id, payload, delivery) VALUES (?, '{}'::jsonb, 'INSTANT')",
                userId);
        jdbc.update("INSERT INTO search_log (query, results_count, user_id) VALUES ('дюна', 1, ?)", userId);

        mockMvc.perform(delete("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearerOf(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isNoContent());

        for (String table : new String[]{"beacons", "notifications", "refresh_tokens", "email_tokens"}) {
            Integer rows = jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE user_id = ?", Integer.class,
                    userId);
            assertThat(rows).as(table).isZero();
        }
        Integer logged = jdbc.queryForObject(
                "SELECT count(*) FROM search_log WHERE query = 'дюна' AND user_id IS NULL", Integer.class);
        assertThat(logged).isPositive();
        String details = jdbc.queryForObject("""
                SELECT details::text FROM system_events
                WHERE details ->> 'event' = 'USER_DELETED' AND details ->> 'userId' = ?""", String.class,
                Long.toString(userId));
        assertThat(details).doesNotContain(email);
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearerOf(session)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn422WhenDeletingAccountWithWrongPassword() throws Exception {
        String email = registered();
        String session = signIn(email);

        mockMvc.perform(delete("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearerOf(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"wrong-password\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Неверный пароль"));
        assertThat(userId(email)).isPositive();
    }

    @Test
    void shouldReturn409WhenAdministratorDeletesAccount() throws Exception {
        String email = registered();
        jdbc.update("UPDATE users SET role = 'ADMIN' WHERE email = ?", email);
        String session = signIn(email);

        mockMvc.perform(delete("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearerOf(session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(userId(email)).isPositive();
    }

    private long userId(String email) {
        Long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        return id == null ? -1 : id;
    }

    private String registered() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        return email;
    }

    private String signIn(String email) throws Exception {
        return login(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}"));
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + token + "\"}"));
    }

    private static String passwordBody(String current, String next, String refreshToken) {
        return "{\"currentPassword\": \"" + current + "\", \"newPassword\": \"" + next + "\""
                + (refreshToken == null ? "" : ", \"refreshToken\": \"" + refreshToken + "\"") + "}";
    }

    private static String refreshTokenOf(String loginBody) {
        return JsonPath.read(loginBody, "$.refreshToken");
    }

    private static String bearerOf(String loginBody) {
        return "Bearer " + JsonPath.<String>read(loginBody, "$.accessToken");
    }
}
