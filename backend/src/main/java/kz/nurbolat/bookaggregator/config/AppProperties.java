package kz.nurbolat.bookaggregator.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Application settings under the {@code app.} prefix.
 *
 * @param baseUrl     public URL of the frontend, used to build links in emails
 * @param googleBooks Google Books API, used for book metadata only
 * @param dev         local development helpers
 */
@Validated
@ConfigurationProperties("app")
public record AppProperties(
        @NotBlank String baseUrl,
        @Valid @DefaultValue GoogleBooks googleBooks,
        @DefaultValue Dev dev
) {

    public record GoogleBooks(
            @DefaultValue("https://www.googleapis.com/books/v1") @NotBlank String baseUrl,
            String apiKey,
            @DefaultValue("5s") @NotNull Duration timeout
    ) {
    }

    /**
     * @param seed load the demo catalog (editions, offers, price history) on startup
     */
    public record Dev(boolean seed) {
    }
}
