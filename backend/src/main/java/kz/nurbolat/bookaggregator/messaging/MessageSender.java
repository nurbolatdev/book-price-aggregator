package kz.nurbolat.bookaggregator.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static kz.nurbolat.bookaggregator.messaging.RabbitTopology.SCHEMA_VERSION;
import static kz.nurbolat.bookaggregator.messaging.RabbitTopology.SCHEMA_VERSION_HEADER;

/**
 * Sends application messages to {@code kr.events} with their id and contract version in the headers.
 * A broker that cannot be reached is logged, not thrown: requests that publish keep working.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    /**
     * @return {@code false} if the message could not be handed to the broker
     */
    public boolean send(String routingKey, UUID messageId, Object message) {
        try {
            rabbitTemplate.convertAndSend(properties.exchange(), routingKey, message, amqpMessage -> {
                amqpMessage.getMessageProperties().setMessageId(messageId.toString());
                amqpMessage.getMessageProperties().setHeader(SCHEMA_VERSION_HEADER, SCHEMA_VERSION);
                return amqpMessage;
            }, new CorrelationData(messageId.toString()));
            return true;
        } catch (AmqpException e) {
            log.error("Could not publish {} message {}: {}", routingKey, messageId, e.getMessage());
            return false;
        }
    }
}
