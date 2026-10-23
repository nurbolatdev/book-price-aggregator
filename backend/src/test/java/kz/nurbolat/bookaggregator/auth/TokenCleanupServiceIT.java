package kz.nurbolat.bookaggregator.auth;

import kz.nurbolat.bookaggregator.security.TokenHasher;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TokenCleanupServiceIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private EmailTokenRepository emailTokens;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MockMvc mockMvc;

    private AuthProperties properties;
    private TokenCleanupService service;
    private User user;

    @BeforeEach
    void setUp() {
        // Tokens of other tests must not change the counts
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM email_tokens");
        // Batches of two: the cleanup has to loop to delete everything
        properties = new AuthProperties(Duration.ofHours(24), Duration.ofHours(1), Duration.ofMinutes(1), Duration.ofDays(90),
                new AuthProperties.Cleanup(Duration.ofDays(30), Duration.ofDays(7), 2));
        service = new TokenCleanupService(refreshTokens, emailTokens, properties, transactionManager, Clock.systemUTC());
        user = userRepository.save(user());
    }

    @Test
    void shouldDeleteOnlyRefreshTokensExpiredLongerThanRetention() {
        refreshToken("old-1", "31 days", false);
        refreshToken("old-2", "40 days", true);
        refreshToken("old-3", "60 days", false);
        String recentlyExpired = refreshToken("recent", "29 days", true);
        String revokedActive = refreshToken("revoked", "-5 days", true);
        String active = refreshToken("active", "-20 days", false);

        TokenCleanupService.Result result = service.cleanUp();

        assertThat(result.refreshTokens()).isEqualTo(3);
        assertThat(jdbc.queryForList("SELECT token_hash FROM refresh_tokens", String.class))
                .containsExactlyInAnyOrder(recentlyExpired, revokedActive, active);
    }

    @Test
    void shouldDeleteEmailLinksExpiredLongerThanRetentionUsedOrNot() {
        emailToken("old-unused", "8 days", false);
        emailToken("old-used", "10 days", true);
        String recentlyUsed = emailToken("recent-used", "6 days", true);
        String active = emailToken("active", "-1 hours", false);

        TokenCleanupService.Result result = service.cleanUp();

        assertThat(result.emailTokens()).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT token_hash FROM email_tokens", String.class))
                .containsExactlyInAnyOrder(recentlyUsed, active);
    }

    @Test
    void shouldDeleteTokensOnlyOnceRetentionHasFullyPassed() {
        Instant now = Instant.parse("2026-10-23T03:30:00Z");
        TokenCleanupService atNow = new TokenCleanupService(refreshTokens, emailTokens, properties, transactionManager,
                Clock.fixed(now, ZoneOffset.UTC));
        String refreshKept = refreshTokenExpiringAt(now.minus(Duration.ofDays(30)).plusSeconds(1));
        refreshTokenExpiringAt(now.minus(Duration.ofDays(30)).minusSeconds(1));
        String emailKept = emailTokenExpiringAt(now.minus(Duration.ofDays(7)).plusSeconds(1));
        emailTokenExpiringAt(now.minus(Duration.ofDays(7)).minusSeconds(1));

        TokenCleanupService.Result result = atNow.cleanUp();

        assertThat(result).isEqualTo(new TokenCleanupService.Result(1, 1));
        assertThat(jdbc.queryForList("SELECT token_hash FROM refresh_tokens", String.class)).containsExactly(refreshKept);
        assertThat(jdbc.queryForList("SELECT token_hash FROM email_tokens", String.class)).containsExactly(emailKept);
    }

    @Test
    void shouldStillDetectReuseOfRevokedTokenAfterCleanup() throws Exception {
        String stolen = TokenHasher.newToken();
        String sibling = TokenHasher.newToken();
        UUID family = UUID.randomUUID();
        insertRefresh(TokenHasher.hash(stolen), family, "-10 days", true);
        insertRefresh(TokenHasher.hash(sibling), family, "-10 days", false);

        service.cleanUp();

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + stolen + "\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT revoked_at IS NOT NULL FROM refresh_tokens WHERE token_hash = ?",
                Boolean.class, TokenHasher.hash(sibling))).isTrue();
    }

    /**
     * @param expiredAgo how long ago the token expired, as a PostgreSQL interval (negative = still valid)
     * @return its hash
     */
    private String refreshToken(String name, String expiredAgo, boolean revoked) {
        String hash = TokenHasher.hash(name + UUID.randomUUID());
        insertRefresh(hash, UUID.randomUUID(), expiredAgo, revoked);
        return hash;
    }

    private void insertRefresh(String hash, UUID family, String expiredAgo, boolean revoked) {
        jdbc.update("""
                INSERT INTO refresh_tokens (user_id, family_id, token_hash, expires_at, revoked_at)
                VALUES (?, ?, ?, NOW() - CAST(? AS INTERVAL), CASE WHEN ? THEN NOW() - INTERVAL '40 days' END)""",
                user.getId(), family, hash, expiredAgo, revoked);
    }

    private String refreshTokenExpiringAt(Instant expiresAt) {
        String hash = TokenHasher.hash(UUID.randomUUID().toString());
        jdbc.update("INSERT INTO refresh_tokens (user_id, family_id, token_hash, expires_at) VALUES (?, ?, ?, ?)",
                user.getId(), UUID.randomUUID(), hash, Timestamp.from(expiresAt));
        return hash;
    }

    private String emailTokenExpiringAt(Instant expiresAt) {
        String hash = TokenHasher.hash(UUID.randomUUID().toString());
        jdbc.update("INSERT INTO email_tokens (user_id, token_hash, purpose, expires_at) VALUES (?, ?, 'VERIFY_EMAIL', ?)",
                user.getId(), hash, Timestamp.from(expiresAt));
        return hash;
    }

    private String emailToken(String name, String expiredAgo, boolean used) {
        String hash = TokenHasher.hash(name + UUID.randomUUID());
        jdbc.update("""
                INSERT INTO email_tokens (user_id, token_hash, purpose, expires_at, used_at)
                VALUES (?, ?, 'RESET_PASSWORD', NOW() - CAST(? AS INTERVAL), CASE WHEN ? THEN NOW() - INTERVAL '10 days' END)""",
                user.getId(), hash, expiredAgo, used);
        return hash;
    }
}
