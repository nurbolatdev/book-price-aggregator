package kz.nurbolat.bookaggregator.user;

import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldReturnOwnProfileWhenSignedIn() throws Exception {
        User user = user();
        user.setName("Айгерим");
        userRepository.save(user);

        mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.name").value("Айгерим"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.notifyFrequency").value("INSTANT"))
                .andExpect(jsonPath("$.weeklyDigest").value(true));
    }

    @Test
    void shouldChangeOnlyGivenFieldsWhenPatching() throws Exception {
        User user = user();
        user.setName("Old");
        userRepository.save(user);

        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifyFrequency\": \"DAILY\", \"weeklyDigest\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Old"))
                .andExpect(jsonPath("$.notifyFrequency").value("DAILY"))
                .andExpect(jsonPath("$.weeklyDigest").value(false));

        User saved = userRepository.findById(user.getId()).orElseThrow();
        assertThat(saved.getNotifyFrequency()).isEqualTo(NotifyFrequency.DAILY);
        assertThat(saved.isWeeklyDigest()).isFalse();
        assertThat(saved.getName()).isEqualTo("Old");
    }

    @Test
    void shouldReturn422WhenNameIsBlankOrFrequencyUnknown() throws Exception {
        User user = userRepository.save(user());

        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"   \"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
        mockMvc.perform(patch("/api/v1/me").header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notifyFrequency\": \"HOURLY\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldReturn401WhenNotSignedIn() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user);
    }
}
