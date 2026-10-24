package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.admin.dto.QueueStats;
import kz.nurbolat.bookaggregator.admin.dto.RequeueResponse;
import kz.nurbolat.bookaggregator.messaging.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminQueueService {

    static final int REQUEUE_LIMIT = 500;
    private static final long CONFIRM_TIMEOUT_MS = 5000;
    private static final MessagePropertiesConverter PROPERTIES_CONVERTER = new DefaultMessagePropertiesConverter();

    private final RabbitAdminClient client;
    private final RabbitAdminProperties properties;
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messaging;
    private final SystemEventService events;
    private final Clock clock;

    private volatile Cached cached;

    /** Queue figures, reused for the configured TTL (15 s) so a refreshing panel does not hammer the broker. */
    public List<QueueStats> queues() {
        Cached current = cached;
        Instant now = clock.instant();
        if (current != null && current.at().plus(properties.cacheTtl()).isAfter(now)) {
            return current.queues();
        }
        List<QueueStats> queues = client.queues();
        cached = new Cached(now, queues);
        return queues;
    }

    /**
     * Moves up to {@value #REQUEUE_LIMIT} messages from the dead-letter queue back to the exchange and
     * routing key they were first published with (from {@code x-death}). A message without that header
     * stays in the dead-letter queue and is counted as skipped.
     */
    public RequeueResponse requeueDeadLetters() {
        String deadLetterQueue = messaging.deadLetterQueue();
        int[] counts = rabbitTemplate.execute(channel -> requeue(channel, deadLetterQueue));
        int requeued = counts[0];
        int skipped = counts[1];
        if (requeued > 0) {
            events.info("queues", "Сообщения возвращены из dead-letter в работу: " + requeued,
                    Map.of("requeued", requeued, "skipped", skipped));
        }
        cached = null;
        return new RequeueResponse(requeued, skipped);
    }

    /**
     * Manual acks: a message is acked only after the broker confirmed its republish, so a crash can duplicate
     * but never lose it. Skipped messages stay unacked until the loop ends and are then returned to the queue,
     * so each message is looked at once per call.
     */
    private int[] requeue(Channel channel, String queue) throws IOException, InterruptedException, TimeoutException {
        int requeued = 0;
        List<Long> skippedTags = new ArrayList<>();
        try {
            channel.confirmSelect();
            int limit = (int) Math.min(REQUEUE_LIMIT, channel.messageCount(queue));
            for (int i = 0; i < limit; i++) {
                GetResponse response = channel.basicGet(queue, false);
                if (response == null) {
                    break;
                }
                long tag = response.getEnvelope().getDeliveryTag();
                Message message = new Message(response.getBody(), PROPERTIES_CONVERTER.toMessageProperties(
                        response.getProps(), response.getEnvelope(), StandardCharsets.UTF_8.name()));
                Map<String, ?> death = firstDeath(message);
                if (death == null) {
                    skippedTags.add(tag);
                    continue;
                }
                channel.basicPublish(String.valueOf(death.get("exchange")), routingKey(death),
                        response.getProps(), response.getBody());
                channel.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
                channel.basicAck(tag, false);
                requeued++;
            }
        } finally {
            // Unacked messages (skipped ones, or the rest after a failure) go back to the queue
            if (channel.isOpen()) {
                for (long tag : skippedTags) {
                    channel.basicNack(tag, false, true);
                }
            }
        }
        return new int[] {requeued, skippedTags.size()};
    }

    private static Map<String, ?> firstDeath(Message message) {
        List<Map<String, ?>> deaths = message.getMessageProperties().getXDeathHeader();
        if (deaths == null || deaths.isEmpty() || deaths.get(0).get("exchange") == null
                || String.valueOf(deaths.get(0).get("exchange")).isEmpty() || routingKey(deaths.get(0)).isEmpty()) {
            return null;
        }
        return deaths.get(0);
    }

    private static String routingKey(Map<String, ?> death) {
        Object keys = death.get("routing-keys");
        if (keys instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }
        return "";
    }

    private record Cached(Instant at, List<QueueStats> queues) {
    }
}
