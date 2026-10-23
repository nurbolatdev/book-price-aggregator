package kz.nurbolat.bookaggregator.migration;

import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the Flyway migrations against fresh databases in the shared PostgreSQL container,
 * including an upgrade of Phase 1 data (state after V3) to the current schema.
 */
class MigrationIT extends AbstractIntegrationTest {

    private static final int POSTGRES_PORT = 5432;

    @Test
    void shouldApplyAllMigrationsWhenDatabaseIsEmpty() throws SQLException {
        DataSource dataSource = createDatabase();

        var result = flyway(dataSource, MigrationVersion.LATEST).migrate();

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(result.success).isTrue();
        assertThat(flyway(dataSource, MigrationVersion.LATEST).info().pending()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stores", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForList("SELECT code FROM stores WHERE NOT enabled", String.class)).containsExactly("KASPI", "WILDBERRIES");
        assertThat(jdbc.queryForObject("SELECT to_regclass('price_alerts') IS NULL", Boolean.class)).isTrue();
    }

    @Test
    void shouldMoveIsbnToEditionsAndAlertsToBeaconsWhenUpgradingPhase1Data() throws SQLException {
        DataSource dataSource = createDatabase();
        flyway(dataSource, MigrationVersion.fromVersion("3")).migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        long userId = insertId(jdbc, "INSERT INTO users (email, password) VALUES ('reader@example.com', 'x') RETURNING id");
        long bookWithIsbn = insertId(jdbc, """
                INSERT INTO books (title, author, isbn, publisher, published_at, cover_url)
                VALUES ('«Мастер и Маргарита»', 'Булгаков, Михаил', '978-0-306-40615-7', 'АСТ', '2019-05-01', 'https://img/1.jpg')
                RETURNING id""");
        long bookWithoutIsbn = insertId(jdbc, "INSERT INTO books (title) VALUES ('Ёлка') RETURNING id");
        long mockOffer = insertId(jdbc, """
                INSERT INTO offers (book_id, source, price, url) VALUES (?, 'kaspi', 4200, 'https://kaspi.kz/1')
                RETURNING id""", bookWithIsbn);
        long flipOffer = insertId(jdbc, """
                INSERT INTO offers (book_id, source, price, original_price, url, fetched_at)
                VALUES (?, 'flip', 3150, 3850, 'https://flip.kz/1', '2026-10-01 10:00:00')
                RETURNING id""", bookWithIsbn);
        jdbc.update("INSERT INTO price_history (offer_id, price) VALUES (?, 4200), (?, 3150)", mockOffer, flipOffer);
        jdbc.update("INSERT INTO price_alerts (user_id, book_id, target_price, active) VALUES (?, ?, 3000, TRUE)",
                userId, bookWithIsbn);
        jdbc.update("""
                INSERT INTO price_alerts (user_id, book_id, target_price, active, triggered_at)
                VALUES (?, ?, 1500, FALSE, '2026-09-01 12:00:00')""", userId, bookWithoutIsbn);

        flyway(dataSource, MigrationVersion.LATEST).migrate();

        Map<String, Object> book = jdbc.queryForMap("SELECT title_norm, author_norm FROM books WHERE id = ?", bookWithIsbn);
        assertThat(book).containsEntry("title_norm", "мастер и маргарита")
                .containsEntry("author_norm", "булгаков михаил");
        assertThat(jdbc.queryForObject("SELECT title_norm FROM books WHERE id = ?", String.class, bookWithoutIsbn))
                .isEqualTo("елка");

        Map<String, Object> edition = jdbc.queryForMap(
                "SELECT id, isbn13, publisher, year, cover_url, binding FROM editions WHERE book_id = ?", bookWithIsbn);
        assertThat(edition).containsEntry("isbn13", "9780306406157")
                .containsEntry("publisher", "АСТ")
                .containsEntry("cover_url", "https://img/1.jpg")
                .containsEntry("binding", "UNKNOWN");
        assertThat(((Number) edition.get("year")).intValue()).isEqualTo(2019);
        assertThat(jdbc.queryForObject("SELECT isbn13 FROM editions WHERE book_id = ?", String.class, bookWithoutIsbn))
                .isNull();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM offers WHERE id = ?", Integer.class, mockOffer)).isZero();
        Map<String, Object> offer = jdbc.queryForMap("""
                SELECT s.code, o.edition_id, o.external_id, o.match_status, o.old_price
                FROM offers o JOIN stores s ON s.id = o.store_id WHERE o.id = ?""", flipOffer);
        assertThat(offer).containsEntry("code", "FLIP")
                .containsEntry("edition_id", edition.get("id"))
                .containsEntry("external_id", "legacy-" + flipOffer)
                .containsEntry("match_status", "MATCHED");
        assertThat((BigDecimal) offer.get("old_price")).isEqualByComparingTo("3850");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM price_history", Integer.class)).isEqualTo(1);

        Map<String, Object> activeBeacon = jdbc.queryForMap("""
                SELECT b.edition_id, b.condition_type, b.target_price, b.status
                FROM beacons b JOIN editions e ON e.id = b.edition_id WHERE e.book_id = ?""", bookWithIsbn);
        assertThat(activeBeacon).containsEntry("edition_id", edition.get("id"))
                .containsEntry("condition_type", "BELOW_PRICE")
                .containsEntry("status", "ACTIVE");
        assertThat((BigDecimal) activeBeacon.get("target_price")).isEqualByComparingTo("3000");

        Map<String, Object> triggeredBeacon = jdbc.queryForMap("""
                SELECT b.status, b.last_triggered_at
                FROM beacons b JOIN editions e ON e.id = b.edition_id WHERE e.book_id = ?""", bookWithoutIsbn);
        assertThat(triggeredBeacon).containsEntry("status", "TRIGGERED");
        assertThat(triggeredBeacon.get("last_triggered_at")).isNotNull();

        assertThat(jdbc.queryForObject("SELECT email_verified FROM users WHERE id = ?", Boolean.class, userId)).isTrue();
    }

    @Test
    void shouldPutAllRefreshTokensOfUserIntoOneFamilyWhenUpgradingFromV13() throws SQLException {
        DataSource dataSource = createDatabase();
        flyway(dataSource, MigrationVersion.fromVersion("13")).migrate();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        long reader = insertId(jdbc, "INSERT INTO users (email, password) VALUES ('reader@example.com', 'x') RETURNING id");
        long other = insertId(jdbc, "INSERT INTO users (email, password) VALUES ('other@example.com', 'x') RETURNING id");
        // A rotated chain of the reader (A revoked, B active) and a token of another user
        jdbc.update("""
                INSERT INTO refresh_tokens (user_id, token_hash, expires_at, revoked_at)
                VALUES (?, 'hash-a', NOW() + INTERVAL '29 days', NOW()),
                       (?, 'hash-b', NOW() + INTERVAL '30 days', NULL),
                       (?, 'hash-c', NOW() + INTERVAL '30 days', NULL)""", reader, reader, other);

        flyway(dataSource, MigrationVersion.LATEST).migrate();

        Map<String, Object> families = jdbc.queryForMap("""
                SELECT (SELECT family_id FROM refresh_tokens WHERE token_hash = 'hash-a')::text AS a,
                       (SELECT family_id FROM refresh_tokens WHERE token_hash = 'hash-b')::text AS b,
                       (SELECT family_id FROM refresh_tokens WHERE token_hash = 'hash-c')::text AS c""");
        // Reusing A then revokes B as well, as before families existed
        assertThat(families.get("a")).isNotNull().isEqualTo(families.get("b"));
        assertThat(families.get("c")).isNotNull().isNotEqualTo(families.get("a"));
    }

    private static Flyway flyway(DataSource dataSource, MigrationVersion target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private static DataSource createDatabase() throws SQLException {
        String name = "migration_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        }
        String url = "jdbc:postgresql://%s:%d/%s".formatted(
                POSTGRES.getHost(), POSTGRES.getMappedPort(POSTGRES_PORT), name);
        return new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static long insertId(JdbcTemplate jdbc, String sql, Object... args) {
        Long id = jdbc.queryForObject(sql, Long.class, args);
        assertThat(id).isNotNull();
        return id;
    }
}
