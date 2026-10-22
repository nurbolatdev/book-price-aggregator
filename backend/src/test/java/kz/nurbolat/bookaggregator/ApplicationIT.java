package kz.nurbolat.bookaggregator;

import kz.nurbolat.bookaggregator.parser.core.BrowserFetcher;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationIT extends AbstractIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldApplyAllMigrationsWhenContextStarts() {
        var info = flyway.info();

        assertThat(info.applied()).isNotEmpty();
        assertThat(info.pending()).isEmpty();
    }

    @Test
    void shouldNotStartBrowserWhenFlagIsOff() {
        assertThat(context.getBeanNamesForType(BrowserFetcher.class)).isEmpty();
    }
}
