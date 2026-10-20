package kz.nurbolat.bookaggregator;

import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationIT extends AbstractIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void shouldApplyAllMigrationsWhenContextStarts() {
        var info = flyway.info();

        assertThat(info.applied()).isNotEmpty();
        assertThat(info.pending()).isEmpty();
    }
}
