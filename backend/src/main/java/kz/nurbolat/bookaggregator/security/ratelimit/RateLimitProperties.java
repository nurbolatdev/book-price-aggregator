package kz.nurbolat.bookaggregator.security.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Request limits ({@code app.rate-limit.*}); each rule allows {@code requests} per {@code period}.
 *
 * @param login          sign-in attempts per IP and email
 * @param register       registrations per IP
 * @param forgotPassword password reset emails per IP
 * @param search         searches per IP
 * @param api            any other API request per user, or per IP when signed out
 */
@Validated
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @Valid @NotNull Limit login,
        @Valid @NotNull Limit register,
        @Valid @NotNull Limit forgotPassword,
        @Valid @NotNull Limit search,
        @Valid @NotNull Limit api
) {

    public record Limit(@Min(1) int requests, @NotNull Duration period) {
    }
}
