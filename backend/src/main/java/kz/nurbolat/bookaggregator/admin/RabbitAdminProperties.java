package kz.nurbolat.bookaggregator.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * RabbitMQ Management HTTP API ({@code app.rabbit-admin.*}), used to show queue sizes in the admin panel.
 *
 * @param url      base URL of the management plugin
 * @param vhost    virtual host whose queues are shown
 * @param cacheTtl how long the queue list is reused between requests
 */
@Validated
@ConfigurationProperties("app.rabbit-admin")
public record RabbitAdminProperties(
        @DefaultValue("http://localhost:15672") @NotBlank String url,
        @DefaultValue("guest") String username,
        @DefaultValue("guest") String password,
        @DefaultValue("/") @NotBlank String vhost,
        @DefaultValue("3s") @NotNull Duration timeout,
        @DefaultValue("15s") @NotNull Duration cacheTtl
) {
}
