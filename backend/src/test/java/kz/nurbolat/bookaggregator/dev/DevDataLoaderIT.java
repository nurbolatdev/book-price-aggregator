package kz.nurbolat.bookaggregator.dev;

import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class DevDataLoaderIT extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldLoadDemoCatalogOnceWhenCalledTwice() {
        DevDataLoader loader = new DevDataLoader(dataSource, jdbc);

        boolean first = loader.load();
        Integer offers = jdbc.queryForObject("SELECT COUNT(*) FROM offers WHERE external_id LIKE 'seed-%'", Integer.class);
        boolean second = loader.load();

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(offers).isGreaterThan(100);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM editions WHERE isbn13 LIKE '97856010%'", Integer.class))
                .isBetween(30, 50);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM offers WHERE external_id LIKE 'seed-%'", Integer.class))
                .isEqualTo(offers);
    }
}
