package kz.nurbolat.bookaggregator.messaging;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Names of the RabbitMQ topology ({@code app.messaging.*}), see docs/01-architecture.md, section 4.
 * Retries of listeners are set with {@code spring.rabbitmq.listener.simple.retry.*}.
 *
 * @param exchange        topic exchange of all application messages
 * @param deadLetterExchange where queues send messages that failed every attempt
 * @param deadLetterQueue the only queue bound to the dead-letter exchange
 */
@Validated
@ConfigurationProperties("app.messaging")
public record MessagingProperties(
        @DefaultValue("kr.events") @NotBlank String exchange,
        @DefaultValue("kr.dlx") @NotBlank String deadLetterExchange,
        @DefaultValue("kr.dead-letter") @NotBlank String deadLetterQueue
) {
}
