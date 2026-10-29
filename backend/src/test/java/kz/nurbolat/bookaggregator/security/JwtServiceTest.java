package kz.nurbolat.bookaggregator.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kz.nurbolat.bookaggregator.user.User;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256";
    private static final Instant NOW = Instant.parse("2026-10-20T09:00:00Z");
    private static final JwtProperties PROPERTIES = new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(30), 10, true, Duration.ofSeconds(10));

    @Test
    void shouldCarryUserIdEmailAndRoleWhenTokenIsFresh() {
        JwtService service = service(NOW);

        AuthenticatedUser user = service.parse(service.generateAccessToken(admin()));

        assertThat(user).isEqualTo(new AuthenticatedUser(7L, "admin@example.com", User.Role.ADMIN));
        assertThat(service.accessTtlSeconds()).isEqualTo(900);
    }

    @Test
    void shouldRejectTokenWhenFifteenMinutesPassed() {
        String token = service(NOW).generateAccessToken(admin());

        assertThat(service(NOW.plus(Duration.ofMinutes(14))).parse(token).id()).isEqualTo(7L);
        assertThatThrownBy(() -> service(NOW.plus(Duration.ofMinutes(16))).parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldRejectTokenWhenSignedWithAnotherKey() {
        JwtService other = new JwtService(new JwtProperties(SECRET + "-other", Duration.ofMinutes(15), Duration.ofDays(30), 10, true, Duration.ofSeconds(10)),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> service(NOW).parse(other.generateAccessToken(admin())))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenWhenItHasNoUserClaims() {
        // Shape of the tokens issued before M18: subject is the email
        String legacy = Jwts.builder()
                .subject("admin@example.com")
                .expiration(java.util.Date.from(NOW.plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> service(NOW).parse(legacy)).isInstanceOf(JwtException.class);
    }

    private static JwtService service(Instant now) {
        return new JwtService(PROPERTIES, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static User admin() {
        User user = new User();
        user.setId(7L);
        user.setEmail("admin@example.com");
        user.setRole(User.Role.ADMIN);
        return user;
    }
}
