package kz.nurbolat.bookaggregator.notification;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Outgoing emails ({@code app.mail.*}); the SMTP server itself is {@code spring.mail.*}.
 *
 * @param from sender shown to the reader, {@code Name <address>}
 */
@Validated
@ConfigurationProperties("app.mail")
public record MailProperties(
        @DefaultValue("KitapRadar <no-reply@kitapradar.kz>") @NotBlank String from
) {
}
