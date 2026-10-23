package kz.nurbolat.bookaggregator.auth;

import com.jayway.jsonpath.JsonPath;
import kz.nurbolat.bookaggregator.security.JwtProperties;
import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.security.TokenHasher;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshTokenIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void shouldIssueNewPairAndRevokeOldTokenWhenRefreshing() throws Exception {
        String first = refreshTokenOf(signIn(registered()));

        String body = refresh(first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        String second = JsonPath.read(body, "$.refreshToken");

        assertThat(second).isNotEqualTo(first);
        assertThat(revokedAt(first)).isNotNull();
        assertThat(revokedAt(second)).isNull();
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(body, "$.accessToken")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRevokeAllSessionsWhenRevokedTokenIsUsedAgain() throws Exception {
        String email = registered();
        String stolen = refreshTokenOf(signIn(email));
        String otherDevice = refreshTokenOf(signIn(email));
        String rotated = JsonPath.read(refresh(stolen).andReturn().getResponse().getContentAsString(), "$.refreshToken");

        refresh(stolen)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        refresh(rotated).andExpect(status().isUnauthorized());
        refresh(otherDevice).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnTokenExpiredWhenRefreshTokenIsTooOld() throws Exception {
        String token = refreshTokenOf(signIn(registered()));
        jdbc.update("UPDATE refresh_tokens SET expires_at = NOW() - INTERVAL '1 minute' WHERE token_hash = ?",
                TokenHasher.hash(token));

        refresh(token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void shouldRevokeTokenWhenLoggingOut() throws Exception {
        String token = refreshTokenOf(signIn(registered()));

        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + token + "\"}"))
                .andExpect(status().isNoContent());

        refresh(token).andExpect(status().isUnauthorized());
        // Unknown tokens are ignored: logging out twice is not an error
        mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + token + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnTokenExpiredWhenAccessTokenIsOlderThan15Minutes() throws Exception {
        User user = userRepository.findByEmail(registered()).orElseThrow();
        Instant issued = Instant.now().minus(Duration.ofMinutes(16));
        String expired = new JwtService(jwtProperties, Clock.fixed(issued, ZoneOffset.UTC)).generateAccessToken(user);

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String registered() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        return email;
    }

    private String signIn(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.USER_AGENT, "RefreshTokenIT")
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + token + "\"}"));
    }

    private Instant revokedAt(String token) {
        return jdbc.queryForObject("SELECT revoked_at FROM refresh_tokens WHERE token_hash = ?", Instant.class,
                TokenHasher.hash(token));
    }

    private static String refreshTokenOf(String loginBody) {
        return JsonPath.read(loginBody, "$.refreshToken");
    }
}
