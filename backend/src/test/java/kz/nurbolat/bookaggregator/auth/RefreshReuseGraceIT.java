package kz.nurbolat.bookaggregator.auth;

import com.jayway.jsonpath.JsonPath;
import kz.nurbolat.bookaggregator.security.TokenHasher;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static kz.nurbolat.bookaggregator.support.RefreshCookieSupport.refreshTokenOf;
import static kz.nurbolat.bookaggregator.support.RefreshCookieSupport.withRefreshCookie;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshReuseGraceIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldAnswerBothParallelRefreshesWithTheSameNewToken() throws Exception {
        String email = registered();
        String t0 = refreshTokenOf(signIn(email));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<MockHttpServletResponse> bodies;
        try {
            Callable<MockHttpServletResponse> call = () -> {
                start.await();
                return refresh(t0).andExpect(status().isOk()).andReturn().getResponse();
            };
            Future<MockHttpServletResponse> first = pool.submit(call);
            Future<MockHttpServletResponse> second = pool.submit(call);
            start.countDown();
            bodies = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        String t1 = refreshTokenOf(bodies.get(0));
        assertThat(refreshTokenOf(bodies.get(1))).isEqualTo(t1).isNotEqualTo(t0);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", userId)).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL", userId))
                .isEqualTo(1);
        refresh(t1).andExpect(status().isOk());
        assertThat(count("SELECT COUNT(*) FROM system_events WHERE source = 'auth' AND level = 'INFO' "
                + "AND details ->> 'userId' = ?", userId.toString())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM system_events WHERE source = 'auth' AND level = 'WARN' "
                + "AND details ->> 'userId' = ?", userId.toString())).isZero();
    }

    @Test
    void shouldNotLeakTokensIntoTheJournalWhenAnsweringInTheWindow() throws Exception {
        String email = registered();
        String t0 = refreshTokenOf(signIn(email));
        String t1 = next(t0);

        refresh(t0).andExpect(status().isOk());

        String details = jdbc.queryForObject("""
                SELECT details::text FROM system_events WHERE source = 'auth' AND level = 'INFO'
                ORDER BY id DESC LIMIT 1""", String.class);
        assertThat(details).doesNotContain(t0, t1, TokenHasher.hash(t0), TokenHasher.hash(t1));
    }

    @Test
    void shouldRevokeFamilyWhenReplayComesAfterTheWindow() throws Exception {
        String email = registered();
        String t0 = refreshTokenOf(signIn(email));
        String t1 = next(t0);
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        shiftRevocation(t0, 11);

        refresh(t0).andExpect(status().isUnauthorized());

        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL", userId))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM system_events WHERE source = 'auth' AND level = 'WARN' "
                + "AND details ->> 'userId' = ?", userId.toString())).isEqualTo(1);
        refresh(t1).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAnswerWithCurrentTokenWhenOlderTokenComesBackAfterTwoRotations() throws Exception {
        String t0 = refreshTokenOf(signIn(registered()));
        String t1 = next(t0);
        String t2 = next(t1);

        MockHttpServletResponse response = refresh(t0).andExpect(status().isOk()).andReturn().getResponse();

        assertThat(refreshTokenOf(response)).isEqualTo(t2);
    }

    @Test
    void shouldRejectReplayWhenSessionWasEndedByLogout() throws Exception {
        String email = registered();
        String t0 = refreshTokenOf(signIn(email));
        String t1 = next(t0);
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();

        mockMvc.perform(withRefreshCookie(post("/api/v1/auth/logout"), t1))
                .andExpect(status().isNoContent());

        refresh(t0).andExpect(status().isUnauthorized());
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL", userId))
                .isZero();
    }

    @Test
    void shouldRejectReplayOfJustRotatedTokenWhenPasswordWasChangedOnAnotherDevice() throws Exception {
        String email = registered();
        MockHttpServletResponse deviceA = signIn(email);
        String deviceB0 = refreshTokenOf(signIn(email));
        next(deviceB0);

        mockMvc.perform(post("/api/v1/me/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(deviceA.getContentAsString(), "$.accessToken"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\": \"" + PASSWORD + "\", \"newPassword\": \"newSecret123\"}"))
                .andExpect(status().isNoContent());

        refresh(deviceB0).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldEndSessionWhenLoggingOutWithTokenRotatedASecondAgo() throws Exception {
        String t0 = refreshTokenOf(signIn(registered()));
        String t1 = next(t0);

        mockMvc.perform(withRefreshCookie(post("/api/v1/auth/logout"), t0))
                .andExpect(status().isNoContent());

        refresh(t1).andExpect(status().isUnauthorized());
    }

    private String next(String token) throws Exception {
        return refreshTokenOf(refresh(token).andExpect(status().isOk()).andReturn().getResponse());
    }

    private void shiftRevocation(String token, int seconds) {
        jdbc.update("UPDATE refresh_tokens SET revoked_at = revoked_at - make_interval(secs => ?) WHERE token_hash = ?",
                seconds, TokenHasher.hash(token));
    }

    private int count(String sql, Object arg) {
        Integer value = jdbc.queryForObject(sql, Integer.class, arg);
        return value == null ? 0 : value;
    }

    private String registered() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        return email;
    }

    private MockHttpServletResponse signIn(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.USER_AGENT, "RefreshReuseGraceIT")
                        .content("{\"email\": \"" + email + "\", \"password\": \"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(withRefreshCookie(post("/api/v1/auth/refresh"), token));
    }
}
