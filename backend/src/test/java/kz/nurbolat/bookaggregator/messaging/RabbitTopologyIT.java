package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static kz.nurbolat.bookaggregator.messaging.RabbitTopology.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The topology on a real broker: every queue exists, routing keys reach the right queues, and a rejected
 * message ends in the dead-letter queue. Listeners are off here, so messages stay in their queues.
 */
class RabbitTopologyIT extends AbstractIntegrationTest {

    private static final List<String> QUEUES = List.of(PARSE_FLIP_QUEUE, PARSE_KASPI_QUEUE, PARSE_MELOMAN_QUEUE,
            PARSE_WILDBERRIES_QUEUE, PARSE_OZON_QUEUE, PRICE_CHANGED_BEACONS_QUEUE, PRICE_CHANGED_CACHE_QUEUE,
            NOTIFY_EMAIL_QUEUE, "kr.dead-letter");

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void emptyQueues() {
        rabbitAdmin.initialize();
        QUEUES.forEach(rabbitAdmin::purgeQueue);
    }

    @Test
    void shouldDeclareAllQueues() {
        assertThat(QUEUES).allSatisfy(queue -> assertThat(rabbitAdmin.getQueueInfo(queue)).as(queue).isNotNull());
    }

    @Test
    void shouldRouteParseRequestOnlyToQueueOfItsStore() {
        rabbitTemplate.convertAndSend("kr.events", "parse.request.ozon", "task");

        assertThat(rabbitTemplate.receiveAndConvert(PARSE_OZON_QUEUE, 5000)).isEqualTo("task");
        assertThat(rabbitAdmin.getQueueInfo(PARSE_FLIP_QUEUE).getMessageCount()).isZero();
    }

    @Test
    void shouldRoutePriceChangeToBeaconsAndCacheQueues() {
        rabbitTemplate.convertAndSend("kr.events", PRICE_CHANGED_KEY, "change");

        assertThat(rabbitTemplate.receiveAndConvert(PRICE_CHANGED_BEACONS_QUEUE, 5000)).isEqualTo("change");
        assertThat(rabbitTemplate.receiveAndConvert(PRICE_CHANGED_CACHE_QUEUE, 5000)).isEqualTo("change");
    }

    @Test
    void shouldRouteEmailNotifications() {
        rabbitTemplate.convertAndSend("kr.events", NOTIFY_EMAIL_KEY, "email");

        assertThat(rabbitTemplate.receiveAndConvert(NOTIFY_EMAIL_QUEUE, 5000)).isEqualTo("email");
    }

    @Test
    void shouldDeadLetterRejectedMessage() {
        rabbitTemplate.convertAndSend("kr.events", NOTIFY_EMAIL_KEY, "poison");

        rabbitTemplate.execute(channel -> {
            // Publishing is asynchronous: wait until the broker has put the message into the queue
            var delivery = channel.basicGet(NOTIFY_EMAIL_QUEUE, false);
            for (int attempt = 0; delivery == null && attempt < 50; attempt++) {
                Thread.sleep(100);
                delivery = channel.basicGet(NOTIFY_EMAIL_QUEUE, false);
            }
            assertThat(delivery).as("message in " + NOTIFY_EMAIL_QUEUE).isNotNull();
            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
            return null;
        });

        Message deadLetter = rabbitTemplate.receive("kr.dead-letter", 5000);
        assertThat(deadLetter).isNotNull();
        assertThat(rabbitTemplate.getMessageConverter().fromMessage(deadLetter)).isEqualTo("poison");
    }
}
