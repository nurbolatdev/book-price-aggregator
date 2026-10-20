package kz.nurbolat.bookaggregator.user;

import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnTokenWhenRegisteringNewEmail() throws Exception {
        String email = uniqueEmail();

        register(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void shouldReturnTokenWhenCredentialsAreValid() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isOk());

        login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void shouldReturn401InErrorFormatWhenPasswordIsWrong() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isOk());

        login(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void shouldReturn409WhenEmailIsAlreadyRegistered() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD).andExpect(status().isOk());

        register(email, PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void shouldReturn401InErrorFormatWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/favorites").header("X-Request-Id", "test-trace-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "test-trace-1"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/favorites"));
    }

    private ResultActions register(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "name": "Test"}
                        """.formatted(email, password)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
