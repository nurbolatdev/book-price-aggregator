package kz.nurbolat.bookaggregator.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kz.nurbolat.bookaggregator.beacon.BeaconEmailRelay;
import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Notification log and retry. Rows are dated in the future so they come first in the shared database. */
@Transactional
class AdminNotificationsIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BeaconEmailRelay relay;

    @PersistenceContext
    private EntityManager entityManager;

    @MockBean
    private RabbitAdminClient rabbitAdminClient;

    @Test
    void shouldListNewestFirstWithTitlesAndEmail() throws Exception {
        long owner = verifiedUser();
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, owner);
        long instant = notification(owner, "INSTANT", "SENT", "{\"title\": \"Шантарам\"}", "3 days");
        long digest = notification(owner, "DIGEST", "QUEUED", "{}", "2 days");
        long weekly = notification(owner, "WEEKLY", "FAILED", "{\"isoWeek\": \"2026-W44\"}", "1 day");

        mockMvc.perform(get("/api/v1/admin/notifications").param("size", "3").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(weekly))
                .andExpect(jsonPath("$.content[0].title").value("Подборка недели"))
                .andExpect(jsonPath("$.content[0].delivery").value("WEEKLY"))
                .andExpect(jsonPath("$.content[0].userEmail").value(email))
                .andExpect(jsonPath("$.content[1].id").value(digest))
                .andExpect(jsonPath("$.content[1].title").value("Сводка за день"))
                .andExpect(jsonPath("$.content[2].id").value(instant))
                .andExpect(jsonPath("$.content[2].title").value("Шантарам"))
                .andExpect(jsonPath("$.content[2].status").value("SENT"));
    }

    @Test
    void shouldFilterByStatusAndDelivery() throws Exception {
        long owner = verifiedUser();
        long failedWeekly = notification(owner, "WEEKLY", "FAILED", "{}", "1 day");
        long failedInstant = notification(owner, "INSTANT", "FAILED", "{\"title\": \"A\"}", "2 days");
        long sent = notification(owner, "INSTANT", "SENT", "{\"title\": \"B\"}", "3 days");

        mockMvc.perform(get("/api/v1/admin/notifications").param("status", "FAILED").param("delivery", "WEEKLY")
                        .param("size", "100").header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(hasItem((int) failedWeekly)))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem((int) failedInstant))))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem((int) sent))))
                .andExpect(jsonPath("$.content[*].delivery").value(not(hasItem("INSTANT"))))
                .andExpect(jsonPath("$.content[*].status").value(not(hasItem("SENT"))));
    }

    @Test
    void shouldRetryFailedNotificationAndLetTheRelayPickItUp() throws Exception {
        long owner = verifiedUser();
        long id = notification(owner, "WEEKLY", "FAILED", """
                {"isoWeek": "2026-W44", "periodText": "19–26 октября", "items": [
                  {"editionId": 1, "title": "Шантарам", "author": "Грегори Дэвид Робертс", "storeName": "Flip.kz",
                   "price": 4000, "previousPrice": 5000, "dropPercent": 20}]}""", "1 day");
        // Given up two days ago: only a fresh queued_at lets the relay take it again.
        jdbc.update("UPDATE notifications SET queued_at = NOW() - INTERVAL '2 days', error = 'NOT_PUBLISHED', "
                + "published_at = NULL WHERE id = ?", id);

        mockMvc.perform(post("/api/v1/admin/notifications/{id}/retry", id).header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.error").doesNotExist());
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT status FROM notifications WHERE id = ?", String.class, id)).isEqualTo("QUEUED");
        assertThat(jdbc.queryForObject("SELECT published_at IS NULL AND error IS NULL FROM notifications WHERE id = ?",
                Boolean.class, id)).isTrue();
        assertThat(jdbc.queryForObject("SELECT queued_at > NOW() - INTERVAL '1 minute' FROM notifications WHERE id = ?",
                Boolean.class, id)).isTrue();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM system_events
                WHERE source = 'admin' AND (details ->> 'notificationId')::bigint = ?""", Long.class, id)).isEqualTo(1);

        // Fresh queued_at: neither republished yet nor given up, whatever created_at says.
        relay.run();
        assertThat(jdbc.queryForObject("SELECT status FROM notifications WHERE id = ?", String.class, id)).isEqualTo("QUEUED");
        jdbc.update("UPDATE notifications SET queued_at = NOW() - INTERVAL '2 minutes' WHERE id = ?", id);
        relay.run();
        assertThat(jdbc.queryForObject("SELECT published_at IS NOT NULL FROM notifications WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    void shouldRefuseToRetryNotificationThatIsNotFailed() throws Exception {
        long owner = verifiedUser();
        long sent = notification(owner, "INSTANT", "SENT", "{}", "1 day");
        long queued = notification(owner, "INSTANT", "QUEUED", "{}", "2 days");

        for (long id : new long[]{sent, queued}) {
            mockMvc.perform(post("/api/v1/admin/notifications/{id}/retry", id).header(HttpHeaders.AUTHORIZATION, admin()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CONFLICT"));
        }
    }

    @Test
    void shouldRefuseToRetryWhenEmailIsNotVerified() throws Exception {
        long owner = jdbc.queryForObject("""
                INSERT INTO users (email, password, email_verified) VALUES ('adm-notif-unv@example.com', 'x', FALSE)
                RETURNING id""", Long.class);
        long id = notification(owner, "INSTANT", "FAILED", "{}", "1 day");

        mockMvc.perform(post("/api/v1/admin/notifications/{id}/retry", id).header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email пользователя не подтверждён"));
    }

    @Test
    void shouldAnswer404ForUnknownNotification() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notifications/{id}/retry", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldForbidRegularUsers() throws Exception {
        String user = token(User.Role.USER);
        mockMvc.perform(get("/api/v1/admin/notifications").header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/notifications/{id}/retry", 1).header(HttpHeaders.AUTHORIZATION, user))
                .andExpect(status().isForbidden());
    }

    private long verifiedUser() {
        return jdbc.queryForObject("""
                INSERT INTO users (email, password, email_verified) VALUES (?, 'x', TRUE) RETURNING id""", Long.class,
                "adm-notif-" + System.nanoTime() + "@example.com");
    }

    /** Dated far in the future minus {@code ago}, so these rows come first and a smaller {@code ago} is newer. */
    private long notification(long userId, String delivery, String status, String payload, String ago) {
        return jdbc.queryForObject("""
                INSERT INTO notifications (user_id, delivery, status, payload, created_at, queued_at)
                VALUES (?, ?, ?, ?::jsonb, NOW() + INTERVAL '10 years' - ?::interval, NOW())
                RETURNING id""", Long.class, userId, delivery, status, payload, ago);
    }

    private String admin() {
        return token(User.Role.ADMIN);
    }

    private String token(User.Role role) {
        User user = user();
        user.setRole(role);
        userRepository.save(user);
        return "Bearer " + jwtService.generateAccessToken(user);
    }
}
