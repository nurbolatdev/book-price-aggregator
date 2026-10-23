package kz.nurbolat.bookaggregator.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * First administrator ({@code app.admin.*}, from {@code APP_ADMIN_EMAIL} and {@code APP_ADMIN_PASSWORD}).
 * Both empty — no administrator is created.
 */
@ConfigurationProperties("app.admin")
public record AdminProperties(String email, String password) {
}
