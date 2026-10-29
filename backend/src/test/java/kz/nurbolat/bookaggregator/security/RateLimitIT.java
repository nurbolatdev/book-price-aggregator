package kz.nurbolat.bookaggregator.security;

import com.jayway.jsonpath.JsonPath;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.rate-limit.enabled=true")
class RateLimitIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAnswer429WithRetryAfterWhenSixthLoginAttemptComesWithinAMinute() throws Exception {
        String ip = randomIp();
        String email = "user-" + UUID.randomUUID() + "@example.com";
        for (int attempt = 1; attempt <= 5; attempt++) {
            login(ip, email).andExpect(status().isUnauthorized());
        }

        login(ip, email)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                // Whole seconds, 1 to 60
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("[1-9]|[1-5][0-9]|60")));

        // Another email from the same address has its own attempts; the body was still read by the controller
        login(ip, "other-" + email).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldAnswer429WhenFourthRegistrationComesFromSameAddress() throws Exception {
        String ip = randomIp();
        for (int i = 1; i <= 3; i++) {
            register(ip).andExpect(status().isCreated());
        }

        register(ip).andExpect(status().isTooManyRequests());
        register(randomIp()).andExpect(status().isCreated());
    }

    @Test
    void shouldLimitFacetsSeparatelyFromSearch() throws Exception {
        String ip = randomIp();
        for (int i = 1; i <= 60; i++) {
            facets(ip).andExpect(status().isOk());
        }

        facets(ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("[1-9]|[1-5][0-9]|60")));
        mockMvc.perform(get("/api/v1/search").with(request -> {
            request.setRemoteAddr(ip);
            return request;
        })).andExpect(status().isOk());
    }

    @Test
    void shouldAnswer429WhenSixthPasswordChangeComesWithinAMinute() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        register(randomIp(), email).andExpect(status().isCreated());
        String access = JsonPath.read(mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> {
                            request.setRemoteAddr(randomIp());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"" + email + "\", \"password\": \"secret123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.accessToken");
        for (int i = 1; i <= 5; i++) {
            changePassword(access).andExpect(status().isUnprocessableEntity());
        }

        changePassword(access)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("[1-9]|[1-5][0-9]|60")));
    }

    private ResultActions changePassword(String access) throws Exception {
        return mockMvc.perform(post("/api/v1/me/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\": \"wrong-password\", \"newPassword\": \"newSecret123\"}"));
    }

    private ResultActions register(String ip, String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"secret123\"}"));
    }

    private ResultActions facets(String ip) throws Exception {
        return mockMvc.perform(get("/api/v1/search/facets").with(request -> {
            request.setRemoteAddr(ip);
            return request;
        }));
    }

    private ResultActions login(String ip, String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\", \"password\": \"wrong-password\"}"));
    }

    private ResultActions register(String ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"user-" + UUID.randomUUID() + "@example.com\", \"password\": \"secret123\"}"));
    }

    private static String randomIp() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "10." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(1, 255);
    }
}
