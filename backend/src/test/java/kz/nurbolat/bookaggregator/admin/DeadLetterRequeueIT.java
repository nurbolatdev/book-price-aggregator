package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.admin.dto.RequeueResponse;
import kz.nurbolat.bookaggregator.messaging.MessagingProperties;
import kz.nurbolat.bookaggregator.messaging.RabbitTopology;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import com.rabbitmq.client.GetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;


import static org.assertj.core.api.Assertions.assertThat;

/** Queue consumers are off in tests, so messages stay where they are published. */
class DeadLetterRequeueIT extends AbstractIntegrationTest {

    @Autowired
    private AdminQueueService queueService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private MessagingProperties messaging;

    @BeforeEach
    void emptyQueues() {
        rabbitAdmin.purgeQueue(RabbitTopology.PARSE_FLIP_QUEUE);
        rabbitAdmin.purgeQueue(messaging.deadLetterQueue());
    }

    @Test
    void shouldReturnDeadLetteredMessageToItsWorkingQueue() {
        // The broker itself adds x-death when a working-queue message is rejected without requeue
        rabbitTemplate.send(messaging.exchange(), "parse.request.flip",
                MessageBuilder.withBody("{\"store\":\"FLIP\"}".getBytes())
                        .setContentType("application/json").build());
        rabbitTemplate.execute(channel -> {
            GetResponse got = null;
            for (int i = 0; i < 50 && got == null; i++) {
                got = channel.basicGet(RabbitTopology.PARSE_FLIP_QUEUE, false);
                if (got == null) {
                    Thread.sleep(100);
                }
            }
            assertThat(got).isNotNull();
            channel.basicNack(got.getEnvelope().getDeliveryTag(), false, false);
            return null;
        });
        awaitDeadLetters(1);

        RequeueResponse response = queueService.requeueDeadLetters();

        assertThat(response).isEqualTo(new RequeueResponse(1, 0));
        Message back = rabbitTemplate.receive(RabbitTopology.PARSE_FLIP_QUEUE, 5000);
        assertThat(back).isNotNull();
        assertThat(new String(back.getBody())).isEqualTo("{\"store\":\"FLIP\"}");
        assertThat(rabbitTemplate.receive(messaging.deadLetterQueue())).isNull();
    }

    @Test
    void shouldKeepMessageWithoutDeathHeaderInDeadLetterQueue() {
        rabbitTemplate.send(messaging.deadLetterExchange(), "",
                MessageBuilder.withBody("orphan".getBytes()).build());

        awaitDeadLetters(1);

        RequeueResponse response = queueService.requeueDeadLetters();

        assertThat(response).isEqualTo(new RequeueResponse(0, 1));
        Message kept = rabbitTemplate.receive(messaging.deadLetterQueue(), 5000);
        assertThat(kept).isNotNull();
        assertThat(new String(kept.getBody())).isEqualTo("orphan");
    }

    @Test
    void shouldCountSkippedMessageOnceAndLeaveItInQueue() {
        for (int i = 0; i < 3; i++) {
            rabbitTemplate.send(messaging.deadLetterExchange(), "",
                    MessageBuilder.withBody(("orphan" + i).getBytes()).build());
        }

        awaitDeadLetters(3);

        RequeueResponse response = queueService.requeueDeadLetters();

        assertThat(response).isEqualTo(new RequeueResponse(0, 3));
        assertThat(rabbitAdmin.getQueueInfo(messaging.deadLetterQueue()).getMessageCount()).isEqualTo(3);
    }

    /** Publishing is asynchronous: wait until the broker reports the messages before requeueing. */
    private void awaitDeadLetters(int expected) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline
                && rabbitAdmin.getQueueInfo(messaging.deadLetterQueue()).getMessageCount() < expected) {
            Thread.onSpinWait();
        }
    }
}
