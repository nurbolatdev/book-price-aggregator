package kz.nurbolat.bookaggregator.beacon;

import com.jayway.jsonpath.JsonPath;
import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.addOffer;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BeaconServiceIT extends AbstractIntegrationTest {

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

    @BeforeEach
    void setUp() {
        token = bearer(userRepository.save(user()));
        edition = editionWithHistory(jdbc, "Мастер и Маргарита", true, daysAgo(3, 3610));
        // A cheaper offer out of stock does not count as the best price
        addOffer(jdbc, edition, "MELOMAN", false, daysAgo(1, 2000));
    }

    @Test
    void shouldCreateBeaconWithCurrentBestPriceAsBaseline() throws Exception {
        create(token, """
                {"editionId": %d, "conditionType": "BELOW_PRICE", "targetPrice": 3200}""".formatted(edition))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.editionId").value(edition))
                .andExpect(jsonPath("$.title").value("Мастер и Маргарита"))
                .andExpect(jsonPath("$.conditionType").value("BELOW_PRICE"))
                .andExpect(jsonPath("$.targetPrice").value(3200))
                .andExpect(jsonPath("$.targetPercent").doesNotExist())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.baselinePrice").value(3610))
                .andExpect(jsonPath("$.currentBestPrice").value(3610))
                .andExpect(jsonPath("$.currentBestStore").value("FLIP"));
    }

    @Test
    void shouldReturn409WhenEditionIsAlreadyWatched() throws Exception {
        String body = """
                {"editionId": %d, "conditionType": "ANY_DROP"}""".formatted(edition);
        create(token, body).andExpect(status().isCreated());

        create(token, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void shouldCreateOneBeaconWhenSameEditionIsWatchedTwiceAtOnce() throws Exception {
        String body = "{\"editionId\": %d, \"conditionType\": \"ANY_DROP\"}".formatted(edition);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> request = () -> {
                start.await();
                return create(token, body).andReturn().getResponse().getStatus();
            };
            var first = pool.submit(request);
            var second = pool.submit(request);
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM beacons WHERE edition_id = ?", Integer.class, edition))
                .isEqualTo(1);
    }

    @Test
    void shouldReturn422WhenConditionLacksItsParameter() throws Exception {
        create(token, """
                {"editionId": %d, "conditionType": "BELOW_PRICE"}""".formatted(edition))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("targetPrice"));
        create(token, """
                {"editionId": %d, "conditionType": "DISCOUNT_PERCENT", "targetPercent": 95}""".formatted(edition))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("targetPercent"));
        create(token, """
                {"editionId": %d, "conditionType": "BELOW_PRICE", "targetPrice": 0}""".formatted(edition))
                .andExpect(status().isUnprocessableEntity());
        create(token, """
                {"editionId": %d, "conditionType": "BELOW_PRICE", "targetPrice": 100}""".formatted(Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAnswer404WhenBeaconBelongsToAnotherUser() throws Exception {
        long id = id(create(token, """
                {"editionId": %d, "conditionType": "ANY_DROP"}""".formatted(edition)));
        String stranger = bearer(userRepository.save(user()));

        mockMvc.perform(patch("/api/v1/beacons/{id}", id).header(HttpHeaders.AUTHORIZATION, stranger)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"PAUSED\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/beacons/{id}", id).header(HttpHeaders.AUTHORIZATION, stranger))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/beacons").header(HttpHeaders.AUTHORIZATION, stranger))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldRearmWithNewBaselineWhenResumedOrConditionChanges() throws Exception {
        long id = id(create(token, """
                {"editionId": %d, "conditionType": "ANY_DROP"}""".formatted(edition)));
        jdbc.update("UPDATE beacons SET status = 'TRIGGERED', last_notified_price = 3000 WHERE id = ?", id);
        jdbc.update("UPDATE offers SET price = 2900 WHERE edition_id = ? AND in_stock", edition);

        update(id, "{\"conditionType\": \"DISCOUNT_PERCENT\", \"targetPercent\": 20}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.targetPercent").value(20))
                .andExpect(jsonPath("$.baselinePrice").value(2900));
        assertThat(jdbc.queryForObject("SELECT last_notified_price FROM beacons WHERE id = ?", Object.class, id)).isNull();

        update(id, "{\"status\": \"PAUSED\"}").andExpect(jsonPath("$.status").value("PAUSED"));
        jdbc.update("UPDATE offers SET price = 2500 WHERE edition_id = ? AND in_stock", edition);
        update(id, "{\"status\": \"ACTIVE\"}")
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.baselinePrice").value(2500));
    }

    @Test
    void shouldRejectSettingTriggeredByHand() throws Exception {
        long id = id(create(token, """
                {"editionId": %d, "conditionType": "ANY_DROP"}""".formatted(edition)));

        update(id, "{\"status\": \"TRIGGERED\"}").andExpect(status().isUnprocessableEntity());
        update(id, "{\"targetPrice\": 100}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("conditionType"));
    }

    @Test
    void shouldListTriggeredFirstWithBestPricesInOneQuery() throws Exception {
        long second = editionWithHistory(jdbc, "Дюна", true, daysAgo(2, 5000));
        long third = editionWithHistory(jdbc, "Шантарам", false, daysAgo(2, 4000));
        id(create(token, "{\"editionId\": %d, \"conditionType\": \"ANY_DROP\"}".formatted(edition)));
        long triggered = id(create(token, "{\"editionId\": %d, \"conditionType\": \"ANY_DROP\"}".formatted(second)));
        id(create(token, "{\"editionId\": %d, \"conditionType\": \"ANY_DROP\"}".formatted(third)));
        jdbc.update("UPDATE beacons SET status = 'TRIGGERED', last_triggered_at = NOW() WHERE id = ?", triggered);

        QueryCountHolder.clear();
        mockMvc.perform(get("/api/v1/beacons").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Дюна"))
                .andExpect(jsonPath("$[0].currentBestPrice").value(5000))
                .andExpect(jsonPath("$[1].title").value("Шантарам"))
                .andExpect(jsonPath("$[1].currentBestPrice").doesNotExist())
                .andExpect(jsonPath("$[2].title").value("Мастер и Маргарита"));
        assertThat(QueryCountHolder.getGrandTotal().getSelect()).isEqualTo(1);

        mockMvc.perform(get("/api/v1/beacons?status=TRIGGERED").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(triggered));
    }

    @Test
    void shouldDeleteOwnBeacon() throws Exception {
        long id = id(create(token, "{\"editionId\": %d, \"conditionType\": \"ANY_DROP\"}".formatted(edition)));

        mockMvc.perform(delete("/api/v1/beacons/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM beacons WHERE id = ?", Integer.class, id)).isZero();
    }

    private ResultActions create(String bearer, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/beacons").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions update(long id, String json) throws Exception {
        return mockMvc.perform(patch("/api/v1/beacons/{id}", id).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static long id(ResultActions created) throws Exception {
        return ((Number) JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user);
    }
}
