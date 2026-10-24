package kz.nurbolat.bookaggregator.admin;

import com.fasterxml.jackson.databind.JsonNode;
import kz.nurbolat.bookaggregator.admin.dto.QueueStats;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reads queue figures from the RabbitMQ Management HTTP API; only the application's {@code kr.*} queues. */
@Component
public class RabbitAdminClient {

    static final String QUEUE_PREFIX = "kr.";

    private final RestClient restClient;
    private final String vhost;

    public RabbitAdminClient(RestClient.Builder builder, RabbitAdminProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.timeout())
                .withReadTimeout(properties.timeout());
        this.restClient = builder
                .baseUrl(properties.url())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .defaultHeaders(headers -> headers.setBasicAuth(properties.username(), properties.password()))
                .build();
        // Encoded by the URI template: "/" becomes %2F as the Management API expects
        this.vhost = properties.vhost();
    }

    /**
     * @throws AppException SERVICE_UNAVAILABLE if the Management API does not answer
     */
    public List<QueueStats> queues() {
        JsonNode body;
        try {
            body = restClient.get().uri("/api/queues/{vhost}", vhost).retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            throw new AppException(ErrorCode.SERVICE_UNAVAILABLE, "RabbitMQ Management API недоступен");
        }
        return parse(body);
    }

    static List<QueueStats> parse(JsonNode body) {
        List<QueueStats> queues = new ArrayList<>();
        if (body != null && body.isArray()) {
            for (JsonNode queue : body) {
                String name = queue.path("name").asText();
                if (name.startsWith(QUEUE_PREFIX)) {
                    queues.add(new QueueStats(name, queue.path("messages_ready").asLong(),
                            queue.path("messages_unacknowledged").asLong(), queue.path("consumers").asInt(),
                            queue.path("message_stats").path("ack_details").path("rate").asDouble(0)));
                }
            }
        }
        queues.sort(Comparator.comparing(QueueStats::name));
        return queues;
    }
}
