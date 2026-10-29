package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PopularQueriesRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private PopularQueriesRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    private void log(String query, String norm, int results, int daysAgo, int times) {
        for (int i = 0; i < times; i++) {
            jdbc.update("""
                    INSERT INTO search_log (query, query_norm, results_count, created_at)
                    VALUES (?, ?, ?, NOW() - make_interval(days => ?))""", query, norm, results, daysAgo);
        }
    }

    @Test
    void shouldIgnoreOldZeroResultAndIsbnQueries() {
        jdbc.update("DELETE FROM search_log");
        log("Свежий", "свежий", 3, 1, 1);
        log("Старый", "старый", 3, 10, 5);
        log("Пустой", "пустой", 0, 1, 5);
        log("9785170000001", "9785170000001", 2, 1, 5);

        List<String> popular = repository.findPopular(Instant.now().minus(7, ChronoUnit.DAYS), 12);

        assertThat(popular).containsExactly("Свежий");
    }

    @Test
    void shouldReturnMostFrequentSpellingOrderedByTotal() {
        jdbc.update("DELETE FROM search_log");
        log("Дюна", "дюна", 3, 1, 2);
        log("дюна ", "дюна", 3, 1, 1);
        log("Эйнштейн", "эйнштейн", 3, 2, 2);
        log("Азбука", "азбука", 3, 2, 1);

        List<String> popular = repository.findPopular(Instant.now().minus(7, ChronoUnit.DAYS), 12);

        assertThat(popular).containsExactly("Дюна", "Эйнштейн", "Азбука");
        assertThat(repository.findPopular(Instant.now().minus(7, ChronoUnit.DAYS), 1)).hasSize(1);
    }
}
