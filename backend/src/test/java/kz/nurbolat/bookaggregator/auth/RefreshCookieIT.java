package kz.nurbolat.bookaggregator.auth;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshCookieIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret123";
    private static final String ALLOWED = "http://localhost:5173";
    private static final String EVIL = "https://evil.example";
    private static final String COOKIE = "kr_refresh";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailTokenService emailTokenService;

    @Test
    void shouldSetHttpOnlyCookieWhenSigningIn() throws Exception {
        MvcResult result = login(registered()).andExpect(status().isOk()).andReturn();

        String cookie = cookieHeader(result);
        assertThat(cookie).startsWith(COOKIE + "=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth", "Max-Age=2592000");
        assertThat(cookie).contains(refreshTokenOf(result));
    }

    @Test
    void shouldRotateCookieWhenRefreshingWithCookieOnly() throws Exception {
        MvcResult login = login(registered()).andReturn();
        String token = refreshTokenOf(login);

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh").header(HttpHeaders.ORIGIN, ALLOWED)
                        .cookie(new Cookie(COOKIE, token)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(refreshTokenOf(refreshed)).isNotEqualTo(token);
        assertThat(cookieHeader(refreshed)).startsWith(COOKIE + "=" + refreshTokenOf(refreshed));
    }

    @Test
    void shouldAcceptBodyTokenWithoutOriginInTransitionalMode() throws Exception {
        String token = refreshTokenOf(login(registered()).andReturn());

        refreshWithBody(token, null).andExpect(status().isOk());
    }

    @Test
    void shouldAcceptBodyTokenWithAllowedOriginAndKeepTokenInResponseBody() throws Exception {
        String token = refreshTokenOf(login(registered()).andReturn());

        refreshWithBody(token, ALLOWED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void shouldPreferCookieOverBodyWhenBothArePresent() throws Exception {
        String email = registered();
        String cookieToken = refreshTokenOf(login(email).andReturn());
        String bodyToken = refreshTokenOf(login(email).andReturn());

        mockMvc.perform(post("/api/v1/auth/refresh").header(HttpHeaders.ORIGIN, ALLOWED)
                        .cookie(new Cookie(COOKIE, cookieToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + bodyToken + "\"}"))
                .andExpect(status().isOk());

        // The cookie token was used up, the body token was not touched
        refreshWithBody(cookieToken, null).andExpect(status().isUnauthorized());
        refreshWithBody(bodyToken, null).andExpect(status().isOk());
    }

    @Test
    void shouldAnswer401WhenRefreshHasNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").header(HttpHeaders.ORIGIN, ALLOWED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectForeignOriginOnRefreshAndLogout() throws Exception {
        String token = refreshTokenOf(login(registered()).andReturn());

        mockMvc.perform(post("/api/v1/auth/refresh").header(HttpHeaders.ORIGIN, EVIL)
                        .cookie(new Cookie(COOKIE, token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.ORIGIN, EVIL)
                        .cookie(new Cookie(COOKIE, token)))
                .andExpect(status().isForbidden());
        // Nothing happened to the session
        refreshWithBody(token, null).andExpect(status().isOk());
    }

    @Test
    void shouldRejectCookieWithoutOrigin() throws Exception {
        String token = refreshTokenOf(login(registered()).andReturn());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(COOKIE, token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/logout").cookie(new Cookie(COOKIE, token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldClearCookieAndEndSessionOnLogout() throws Exception {
        String token = refreshTokenOf(login(registered()).andReturn());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.ORIGIN, ALLOWED)
                        .cookie(new Cookie(COOKIE, token)))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(cookieHeader(result)).startsWith(COOKIE + "=;").contains("Max-Age=0", "Path=/api/v1/auth");
        refreshWithBody(token, null).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAnswer204AndClearCookieWhenLogoutHasNoToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.ORIGIN, ALLOWED))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(cookieHeader(result)).contains("Max-Age=0");
    }

    @Test
    void shouldClearCookieOnLogoutAll() throws Exception {
        MvcResult login = login(registered()).andReturn();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout-all").header(HttpHeaders.AUTHORIZATION, bearerOf(login)))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(cookieHeader(result)).contains("Max-Age=0");
    }

    @Test
    void shouldClearCookieOnPasswordReset() throws Exception {
        User user = userRepository.findByEmail(registered()).orElseThrow();
        String token = emailTokenService.issue(user, EmailTokenPurpose.RESET_PASSWORD, Duration.ofHours(1));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"" + token + "\", \"newPassword\": \"another-secret1\"}"))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(cookieHeader(result)).contains("Max-Age=0");
    }

    @Test
    void shouldClearCookieWhenAccountIsDeleted() throws Exception {
        MvcResult login = login(registered()).andReturn();

        MvcResult result = mockMvc.perform(delete("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearerOf(login))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(cookieHeader(result)).contains("Max-Age=0");
    }

    @Test
    void shouldKeepCookieAndCurrentSessionWhenPasswordChanges() throws Exception {
        String email = registered();
        MvcResult current = login(email).andReturn();
        MvcResult other = login(email).andReturn();

        MvcResult result = mockMvc.perform(post("/api/v1/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearerOf(current))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"" + PASSWORD + "\", \"newPassword\": \"newSecret123\"}"))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        refreshWithBody(refreshTokenOf(current), null).andExpect(status().isOk());
        refreshWithBody(refreshTokenOf(other), null).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowCredentialsOnPreflightFromAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/refresh")
                        .header(HttpHeaders.ORIGIN, ALLOWED)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED));
    }

    private String registered() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        return email;
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"));
    }

    private ResultActions refreshWithBody(String token, String origin) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\": \"" + token + "\"}");
        if (origin != null) {
            request.header(HttpHeaders.ORIGIN, origin);
        }
        return mockMvc.perform(request);
    }

    private static String cookieHeader(MvcResult result) {
        List<String> cookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(1);
        return cookies.get(0);
    }

    private static String refreshTokenOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    private static String bearerOf(MvcResult result) throws Exception {
        return "Bearer " + JsonPath.<String>read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
