package kz.nurbolat.bookaggregator.admin;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import kz.nurbolat.bookaggregator.admin.dto.QueueStats;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The client runs against a local WireMock server, never a real broker. */
class RabbitAdminClientTest {

    private static final String QUEUES = """
            [
              {"name": "kr.parse.flip", "messages_ready": 12, "messages_unacknowledged": 1, "consumers": 1,
               "message_stats": {"ack_details": {"rate": 0.8}}},
              {"name": "kr.dead-letter", "messages_ready": 3, "messages_unacknowledged": 0, "consumers": 0},
              {"name": "amq.gen-abc", "messages_ready": 99, "messages_unacknowledged": 0, "consumers": 0}
            ]
            """;

    private WireMockServer server;
    private RabbitAdminClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        client = new RabbitAdminClient(RestClient.builder(), new RabbitAdminProperties(
                "http://localhost:" + server.port(), "guest", "secret", "/", Duration.ofSeconds(2), Duration.ofSeconds(15)));
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void shouldParseApplicationQueuesWithBasicAuth() {
        server.stubFor(get(urlPathEqualTo("/api/queues/%2F")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json").withBody(QUEUES)));

        List<QueueStats> queues = client.queues();

        assertThat(queues).containsExactly(
                new QueueStats("kr.dead-letter", 3, 0, 0, 0.0),
                new QueueStats("kr.parse.flip", 12, 1, 1, 0.8));
        server.verify(getRequestedFor(urlPathEqualTo("/api/queues/%2F"))
                .withHeader("Authorization", equalTo("Basic Z3Vlc3Q6c2VjcmV0")));
    }

    @Test
    void shouldAnswerServiceUnavailableWhenManagementApiFails() {
        server.stubFor(get(urlPathEqualTo("/api/queues/%2F")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.queues())
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE));
    }
}
