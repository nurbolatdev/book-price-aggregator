package kz.nurbolat.bookaggregator.parser.core;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Store parsing settings ({@code app.parser.*}); see docs/05-parsing-policy.md.
 *
 * @param userAgent                  honest bot identification; HTTP headers must be ASCII
 * @param timeout                    connect and read timeout of one request
 * @param htmlCacheTtl               how long a downloaded page is reused from Redis
 * @param robotsTtl                  how long a store's robots.txt is reused
 * @param maxEditionsPerRun          known offers refreshed in one run
 * @param maxDiscoveryPerRun         editions looked up in one run
 * @param retryWait                  pause before retrying a failed request
 * @param circuitBreakerOpenDuration pause after a store started failing or blocked us
 * @param rateLimitMaxWait           longest wait for a free request slot of a store
 */
@Validated
@ConfigurationProperties("app.parser")
public record ParserProperties(
        @DefaultValue("KitapRadarBot/1.0 (+https://github.com/nurbolatdev/book-price-aggregator; educational project)")
        @NotBlank String userAgent,
        @DefaultValue("10s") Duration timeout,
        @DefaultValue("1h") Duration htmlCacheTtl,
        @DefaultValue("24h") Duration robotsTtl,
        @DefaultValue("200") @Positive int maxEditionsPerRun,
        @DefaultValue("50") @Positive int maxDiscoveryPerRun,
        @DefaultValue("1s") Duration retryWait,
        @DefaultValue("30m") Duration circuitBreakerOpenDuration,
        @DefaultValue("5m") Duration rateLimitMaxWait
) {
}
