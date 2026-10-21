package kz.nurbolat.bookaggregator.dev;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Loads the demo catalog (editions, offers, price history) for manual testing and the frontend
 * until store parsers exist. Enabled with {@code app.dev.seed=true}; runs once per database.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.dev", name = "seed", havingValue = "true")
@RequiredArgsConstructor
public class DevDataLoader implements ApplicationRunner {

    static final String SCRIPT = "db/dev/catalog-seed.sql";

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        load();
    }

    /**
     * @return {@code true} if the catalog was loaded, {@code false} if it was already there
     */
    public boolean load() {
        Boolean loaded = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM offers WHERE external_id LIKE 'seed-%')", Boolean.class);
        if (Boolean.TRUE.equals(loaded)) {
            log.info("Demo catalog already loaded");
            return false;
        }
        new ResourceDatabasePopulator(new ClassPathResource(SCRIPT)).execute(dataSource);
        log.info("Demo catalog loaded from {}", SCRIPT);
        return true;
    }
}
