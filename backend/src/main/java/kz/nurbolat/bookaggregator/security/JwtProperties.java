package kz.nurbolat.bookaggregator.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Tokens of signed-in users ({@code app.jwt.*}).
 *
 * @param secret     HMAC key of access tokens, at least 32 bytes
 * @param accessTtl  life of an access token; short, since it cannot be revoked
 * @param refreshTtl life of a refresh token; each refresh issues a new one
 * @param maxSessions signed-in devices per user; a new sign-in over the limit ends the least recently used one
 * @param refreshCookieSecure whether the refresh cookie carries {@code Secure}; off only on a developer machine without TLS
 * @param refreshReuseGrace how long after a rotation the replaced token is still answered with the current one (parallel tabs)
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @DefaultValue("15m") @NotNull Duration accessTtl,
        @DefaultValue("30d") @NotNull Duration refreshTtl,
        @DefaultValue("10") @Min(1) int maxSessions,
        @DefaultValue("true") boolean refreshCookieSecure,
        @DefaultValue("10s") @NotNull Duration refreshReuseGrace
) {
}
