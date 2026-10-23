package kz.nurbolat.bookaggregator.auth;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Links sent by email ({@code app.auth.*}).
 *
 * @param verifyEmailTtl   life of the link that confirms an address
 * @param resetPasswordTtl life of the link that sets a new password
 * @param resendInterval   least time between two confirmation emails to one user
 */
@Validated
@ConfigurationProperties("app.auth")
public record AuthProperties(
        @DefaultValue("24h") @NotNull Duration verifyEmailTtl,
        @DefaultValue("1h") @NotNull Duration resetPasswordTtl,
        @DefaultValue("1m") @NotNull Duration resendInterval
) {
}
