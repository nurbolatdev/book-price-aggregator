package kz.nurbolat.bookaggregator.beacon;

import kz.nurbolat.bookaggregator.auth.EmailTokenPurpose;
import kz.nurbolat.bookaggregator.auth.EmailTokenService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UnsubscribeIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BeaconRepository beaconRepository;

    @Autowired
    private EmailTokenService emailTokenService;

    @Test
    void shouldPauseBeaconWithoutSigningInAndAllowClickingAgain() throws Exception {
        Beacon beacon = beacon();
        String token = emailTokenService.issueUnsubscribe(beacon, Duration.ofDays(90));

        unsubscribe(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beaconId").value(beacon.getId()))
                .andExpect(jsonPath("$.title").value("Мастер и Маргарита"))
                .andExpect(jsonPath("$.status").value("PAUSED"));
        assertThat(jdbc.queryForObject("SELECT status FROM beacons WHERE id = ?", String.class, beacon.getId()))
                .isEqualTo("PAUSED");

        unsubscribe(token).andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenTokenIsUnknownOrNotAnUnsubscribeLink() throws Exception {
        Beacon beacon = beacon();
        String resetToken = emailTokenService.issue(beacon.getUser(), EmailTokenPurpose.RESET_PASSWORD, Duration.ofHours(1));

        unsubscribe("not-a-real-token")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
        unsubscribe(resetToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
        assertThat(jdbc.queryForObject("SELECT status FROM beacons WHERE id = ?", String.class, beacon.getId()))
                .isEqualTo("ACTIVE");
    }

    @Test
    void shouldReturnTokenExpiredWhenLinkIsOlderThan90Days() throws Exception {
        String token = emailTokenService.issueUnsubscribe(beacon(), Duration.ofSeconds(-1));

        unsubscribe(token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    private ResultActions unsubscribe(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/beacons/unsubscribe").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"" + token + "\"}"));
    }

    private Beacon beacon() {
        User user = userRepository.save(user());
        long edition = editionWithHistory(jdbc, "Мастер и Маргарита", true, daysAgo(1, 3000));
        Long id = jdbc.queryForObject("""
                INSERT INTO beacons (user_id, edition_id, condition_type) VALUES (?, ?, 'ANY_DROP') RETURNING id""",
                Long.class, user.getId(), edition);
        Beacon beacon = beaconRepository.findById(id).orElseThrow();
        beacon.setUser(user);
        return beacon;
    }
}
