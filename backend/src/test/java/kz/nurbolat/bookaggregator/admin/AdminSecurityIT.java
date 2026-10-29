package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every admin endpoint is closed to regular users and open to administrators. */
@Transactional
@AutoConfigureObservability
class AdminSecurityIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    /** No Management API in tests. */
    @MockBean
    private RabbitAdminClient rabbitAdminClient;

    @ParameterizedTest
    @ValueSource(strings = {"/stats", "/activity", "/health", "/parsers", "/queues", "/events",
            "/search/top", "/search/zero-results", "/users", "/notifications", "/offers/pending-review"})
    void shouldForbidUsersAndAllowAdminsOnReads(String path) throws Exception {
        String url = "/api/v1/admin" + path;

        mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, token(User.Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldForbidUsersAndAllowAdminsOnWrites() throws Exception {
        mockMvc.perform(post("/api/v1/admin/queues/dead-letter/requeue")
                        .header(HttpHeaders.AUTHORIZATION, token(User.Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/queues/dead-letter/requeue")
                        .header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN)))
                .andExpect(status().isOk());

        String body = "{\"enabled\": true}";
        mockMvc.perform(patch("/api/v1/admin/stores/FLIP").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(HttpHeaders.AUTHORIZATION, token(User.Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/admin/stores/FLIP").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldKeepPrometheusForAdminsOnly() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").header(HttpHeaders.AUTHORIZATION, token(User.Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/prometheus").header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    private String token(User.Role role) {
        User user = user();
        user.setRole(role);
        userRepository.save(user);
        return "Bearer " + jwtService.generateAccessToken(user);
    }
}
