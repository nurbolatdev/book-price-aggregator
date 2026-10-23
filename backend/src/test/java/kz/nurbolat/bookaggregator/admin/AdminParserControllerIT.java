package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.messaging.RabbitTopology;
import kz.nurbolat.bookaggregator.messaging.message.ParseRequestMessage;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access rules of the manual run and the queued task; actual crawling is covered by StoreCrawlServiceIT
 * without network.
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
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

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
    void shouldQueueCrawlAndAnswerAccepted() throws Exception {
        rabbitAdmin.purgeQueue(RabbitTopology.PARSE_FLIP_QUEUE);

        mockMvc.perform(post("/api/v1/admin/parsers/FLIP/run")
                        .header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"editionIds\": [5, 6]}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.queued").value(true));

        // Listeners are off in tests, so the task waits in the store's queue
        Object queued = rabbitTemplate.receiveAndConvert(RabbitTopology.PARSE_FLIP_QUEUE, 5000);
        assertThat(queued).isInstanceOfSatisfying(ParseRequestMessage.class, message -> {
            assertThat(message.store()).isEqualTo(StoreCode.FLIP);
            assertThat(message.trigger()).isEqualTo(TriggerType.MANUAL);
            assertThat(message.editionIds()).containsExactly(5L, 6L);
        });
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
        return "Bearer " + jwtService.generateAccessToken(user);
    }
}
