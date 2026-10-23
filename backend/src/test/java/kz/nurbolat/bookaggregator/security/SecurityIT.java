package kz.nurbolat.bookaggregator.security;

import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldForbidAdminApiWhenUserIsNotAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/offers/pending-review").header(HttpHeaders.AUTHORIZATION, bearer(User.Role.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/admin/offers/pending-review"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/offers/pending-review").header(HttpHeaders.AUTHORIZATION, bearer(User.Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldShowOnlyStatusOfPublicHealthAndDetailsToAdmin() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, bearer(User.Role.ADMIN)))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void shouldKeepOtherActuatorEndpointsForAdmins() throws Exception {
        mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/info").header(HttpHeaders.AUTHORIZATION, bearer(User.Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/info").header(HttpHeaders.AUTHORIZATION, bearer(User.Role.ADMIN)))
                .andExpect(status().isOk());
    }

    private String bearer(User.Role role) {
        User user = user();
        user.setRole(role);
        userRepository.save(user);
        return "Bearer " + jwtService.generateAccessToken(user);
    }
}
