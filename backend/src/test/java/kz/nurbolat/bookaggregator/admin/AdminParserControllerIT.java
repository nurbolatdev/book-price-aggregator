package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access rules of the manual run; actual crawling is covered by StoreCrawlServiceIT without network.
 */
@Transactional
class AdminParserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/admin/parsers/FLIP/run"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldForbidRegularUsers() throws Exception {
        mockMvc.perform(post("/api/v1/admin/parsers/FLIP/run").header(HttpHeaders.AUTHORIZATION, token(User.Role.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldAnswer404ForStoreWithoutParser() throws Exception {
        mockMvc.perform(post("/api/v1/admin/parsers/KASPI/run").header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No parser for store KASPI"));
    }

    @Test
    void shouldRejectTooManyEditions() throws Exception {
        String ids = "[" + "1,".repeat(200) + "1]";
        mockMvc.perform(post("/api/v1/admin/parsers/FLIP/run")
                        .header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"editionIds\": " + ids + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("editionIds"));
    }

    private String token(User.Role role) {
        User user = user();
        user.setRole(role);
        userRepository.save(user);
        return "Bearer " + jwtService.generateToken(userDetailsService.loadUserByUsername(user.getEmail()));
    }
}
