package kz.nurbolat.bookaggregator.messaging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MessageSenderTest {

    private static final UUID ID = UUID.fromString("7f0c9a8e-3b8d-4c5e-9a71-2f4b6d0c1e33");

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final MessageSender sender = new MessageSender(rabbitTemplate,
            new MessagingProperties("kr.events", "kr.dlx", "kr.dead-letter"));

    @Test
    void shouldSendToEventsExchangeWithIdAndSchemaVersion() {
        boolean sent = sender.send("parse.request.flip", ID, "payload");

        ArgumentCaptor<MessagePostProcessor> headers = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq("kr.events"), eq("parse.request.flip"), eq("payload"),
                headers.capture(), any(CorrelationData.class));
        Message message = headers.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertThat(sent).isTrue();
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(ID.toString());
        assertThat(message.getMessageProperties().<Integer>getHeader("x-schema-version")).isEqualTo(1);
    }

    @Test
    void shouldReportFailureInsteadOfThrowingWhenBrokerIsDown() {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        assertThat(sender.send("price.changed", ID, "payload")).isFalse();
    }
}
