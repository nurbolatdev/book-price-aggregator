package kz.nurbolat.bookaggregator.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Links sent by email and the lifetime of stored tokens ({@code app.auth.*}).
 *
 * @param verifyEmailTtl   life of the link that confirms an address
 * @param resetPasswordTtl life of the link that sets a new password
 * @param resendInterval   least time between two confirmation emails to one user
 * @param unsubscribeTtl   life of the link in beacon emails that pauses the beacon
 * @param cleanup          when expired tokens are deleted
 */
@Validated
@ConfigurationProperties("app.auth")
public record AuthProperties(
        @DefaultValue("24h") @NotNull Duration verifyEmailTtl,
        @DefaultValue("1h") @NotNull Duration resetPasswordTtl,
        @DefaultValue("1m") @NotNull Duration resendInterval,
        @DefaultValue("90d") @NotNull Duration unsubscribeTtl,
        @Valid @DefaultValue Cleanup cleanup
) {

    /**
     * Tokens are kept for a while after they expire, for support questions and investigations; what happened
     * to a session (reuse, password reset) is recorded in {@code system_events}, not in the token tables.
     *
     * @param refreshTokensKeep refresh tokens are deleted this long after they expire, revoked or not
     * @param emailTokensKeep   email links are deleted this long after they expire, used or not
     * @param batchSize         rows deleted per statement, so that no delete holds locks for long
     */
    public record Cleanup(
            @DefaultValue("30d") @NotNull Duration refreshTokensKeep,
            @DefaultValue("7d") @NotNull Duration emailTokensKeep,
            @DefaultValue("1000") @Min(1) int batchSize
    ) {
    }
}
