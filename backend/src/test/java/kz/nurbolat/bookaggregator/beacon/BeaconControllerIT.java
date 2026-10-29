package kz.nurbolat.bookaggregator.beacon;

import com.jayway.jsonpath.JsonPath;
import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BeaconControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String token;
    private long edition;
    private long otherEdition;

    @BeforeEach
    void setUp() {
        token = bearer(userRepository.save(user()));
        edition = editionWithHistory(jdbc, "Мастер и Маргарита", true, daysAgo(3, 3610));
        otherEdition = editionWithHistory(jdbc, "Дюна", true, daysAgo(3, 5000));
    }

    @Test
    void shouldFilterByEditionAndOnlyOwnBeacons() throws Exception {
        long mine = id(create(token, edition));
        create(token, otherEdition);
        create(bearer(userRepository.save(user())), edition);

        mockMvc.perform(get("/api/v1/beacons").param("editionId", String.valueOf(edition))
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(mine));
        mockMvc.perform(get("/api/v1/beacons").param("editionId", "999999")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturnBestUrlAndTimestamps() throws Exception {
        create(token, edition)
                .andExpect(jsonPath("$.currentBestUrl").value(startsWith("http")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.statusChangedAt").exists());
    }

    @Test
    void shouldMoveStatusTimeOnPauseAndResumeButNotOnConditionChange() throws Exception {
        long id = id(create(token, edition));
        setStatusChangedAt(id, "2026-01-01T00:00:00Z");

        update(id, "{\"conditionType\": \"BELOW_PRICE\", \"targetPrice\": 3000}");
        assertThat(statusChangedAt(id)).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));

        update(id, "{\"status\": \"PAUSED\"}");
        Instant paused = statusChangedAt(id);
        assertThat(paused).isAfter(Instant.parse("2026-01-01T00:00:00Z"));

        setStatusChangedAt(id, "2026-01-01T00:00:00Z");
        update(id, "{\"status\": \"PAUSED\"}");
        assertThat(statusChangedAt(id)).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));

        update(id, "{\"status\": \"ACTIVE\"}");
        assertThat(statusChangedAt(id)).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
    }

    private void setStatusChangedAt(long id, String instant) {
        jdbc.update("UPDATE beacons SET status_changed_at = ?::timestamptz WHERE id = ?", instant, id);
    }

    private Instant statusChangedAt(long id) {
        return jdbc.queryForObject("SELECT status_changed_at FROM beacons WHERE id = ?", java.sql.Timestamp.class, id)
                .toInstant();
    }

    private ResultActions create(String bearer, long editionId) throws Exception {
        return mockMvc.perform(post("/api/v1/beacons").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"editionId\": %d, \"conditionType\": \"ANY_DROP\"}".formatted(editionId)));
    }

    private void update(long id, String json) throws Exception {
        mockMvc.perform(patch("/api/v1/beacons/{id}", id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    private static long id(ResultActions created) throws Exception {
        return ((Number) JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user);
    }
}
