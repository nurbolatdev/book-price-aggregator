package kz.nurbolat.bookaggregator.security;

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
 */
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @DefaultValue("15m") @NotNull Duration accessTtl,
        @DefaultValue("30d") @NotNull Duration refreshTtl
) {
}
