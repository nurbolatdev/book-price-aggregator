package kz.nurbolat.bookaggregator.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.nurbolat.bookaggregator.store.StoreCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static kz.nurbolat.bookaggregator.messaging.RabbitTopology.*;

/**
 * RabbitMQ topology (docs/01-architecture.md, section 4): the topic exchange {@code kr.events}, a queue per
 * store for parse requests, two queues for price changes, one for emails; every queue dead-letters into
 * {@code kr.dlx} → {@code kr.dead-letter}. Messages are JSON; publishing waits for broker confirms.
 */
@Slf4j
@Configuration
public class RabbitTopologyConfig {

    @Bean
    Declarables rabbitTopology(MessagingProperties properties) {
        TopicExchange events = new TopicExchange(properties.exchange(), true, false);
        FanoutExchange deadLetters = new FanoutExchange(properties.deadLetterExchange(), true, false);
        Queue deadLetterQueue = QueueBuilder.durable(properties.deadLetterQueue()).build();

        List<Declarable> declarables = new ArrayList<>(List.of(events, deadLetters, deadLetterQueue,
                BindingBuilder.bind(deadLetterQueue).to(deadLetters)));
        for (StoreCode store : StoreCode.values()) {
            Queue queue = queue(parseQueue(store), properties);
            declarables.add(queue);
            declarables.add(bind(queue, events, parseRoutingKey(store)));
        }
        for (String name : List.of(PRICE_CHANGED_BEACONS_QUEUE, PRICE_CHANGED_CACHE_QUEUE)) {
            Queue queue = queue(name, properties);
            declarables.add(queue);
            declarables.add(bind(queue, events, PRICE_CHANGED_KEY));
        }
        Queue email = queue(NOTIFY_EMAIL_QUEUE, properties);
        declarables.add(email);
        declarables.add(bind(email, events, NOTIFY_EMAIL_KEY));
        return new Declarables(declarables);
    }

    /** JSON with the application's ObjectMapper (Java time as ISO-8601). */
    @Bean
    MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /** After the last retry a message is rejected without requeue, so its queue dead-letters it. */
    @Bean
    MessageRecoverer messageRecoverer() {
        return new RejectAndDontRequeueRecoverer();
    }

    /** Logs messages the broker did not accept or could not route (publisher confirms and returns). */
    @Bean
    RabbitTemplateCustomizer confirmLogging() {
        return template -> {
            template.setConfirmCallback((correlation, ack, cause) -> {
                if (!ack) {
                    log.error("Broker did not accept message {}: {}", correlation == null ? null : correlation.getId(), cause);
                }
            });
            template.setReturnsCallback(returned -> log.error("Message to {} / {} was not routed: {} {}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText()));
        };
    }

    private static Queue queue(String name, MessagingProperties properties) {
        return QueueBuilder.durable(name).deadLetterExchange(properties.deadLetterExchange()).build();
    }

    private static Binding bind(Queue queue, TopicExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
