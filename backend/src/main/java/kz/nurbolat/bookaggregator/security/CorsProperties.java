package kz.nurbolat.bookaggregator.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Browser origins allowed to call the API ({@code app.cors.*}).
 *
 * @param allowedOrigins frontend addresses, e.g. {@code http://localhost:5173} (Vite) or {@code http://localhost} (docker)
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
}
