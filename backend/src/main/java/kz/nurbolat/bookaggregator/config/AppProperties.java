package kz.nurbolat.bookaggregator.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application settings under the {@code app.} prefix.
 *
 * @param baseUrl public URL of the frontend, used to build links in emails
 */
@Validated
@ConfigurationProperties("app")
public record AppProperties(@NotBlank String baseUrl) {
}
