package kz.nurbolat.bookaggregator.parser.core;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Headless browser for stores whose pages are rendered by JavaScript ({@code app.parser.browser.*}).
 *
 * @param enabled        the browser starts only when this is true; off by default and in tests
 * @param timeout        longest wait for a page to load, including the content a parser waits for
 * @param executablePath Chromium to use instead of the one Playwright installed, empty for that one
 */
@ConfigurationProperties("app.parser.browser")
public record BrowserProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("30s") Duration timeout,
        String executablePath
) {
}
