package kz.nurbolat.bookaggregator.home;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Home page settings.
 *
 * @param popularFallback queries that pad the popular list while the search log is short
 */
@ConfigurationProperties("app.home")
public record HomeProperties(List<String> popularFallback) {

    public HomeProperties {
        popularFallback = popularFallback == null ? List.of() : List.copyOf(popularFallback);
    }
}
