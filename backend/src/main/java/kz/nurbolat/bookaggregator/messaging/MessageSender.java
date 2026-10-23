package kz.nurbolat.bookaggregator.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

    /**
     * Sends and waits for the broker: the message counts as published only if RabbitMQ confirmed it (ack) and
     * did not return it as unroutable ({@code mandatory}). A nack, a return, a timeout or a broken connection
     * mean "not published". A confirmation lost after the broker took the message also reads as "not published",
     * so a caller that retries may publish twice — consumers must tolerate a repeat.
     *
     * @return {@code true} if the message is in a queue
     */
    public boolean sendConfirmed(String routingKey, UUID messageId, Object message, Duration timeout) {
        CorrelationData correlation = new CorrelationData(messageId.toString());
        try {
            rabbitTemplate.convertAndSend(properties.exchange(), routingKey, message, amqpMessage -> {
                amqpMessage.getMessageProperties().setMessageId(messageId.toString());
                amqpMessage.getMessageProperties().setHeader(SCHEMA_VERSION_HEADER, SCHEMA_VERSION);
                return amqpMessage;
            }, correlation);
            Confirm confirm = correlation.getFuture().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                log.error("Broker refused {} message {}: {}", routingKey, messageId, confirm.getReason());
                return false;
            }
            // A returned message is set before the confirmation completes
            if (correlation.getReturned() != null) {
                log.error("{} message {} reached no queue", routingKey, messageId);
                return false;
            }
            return true;
        } catch (AmqpException | ExecutionException e) {
            log.error("Could not publish {} message {}: {}", routingKey, messageId, e.getMessage());
            return false;
        } catch (TimeoutException e) {
            log.error("No confirmation for {} message {} within {}", routingKey, messageId, timeout);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
