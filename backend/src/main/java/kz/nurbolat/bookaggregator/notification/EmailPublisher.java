package kz.nurbolat.bookaggregator.notification;

import kz.nurbolat.bookaggregator.common.text.Emails;
import kz.nurbolat.bookaggregator.messaging.MessageSender;
import kz.nurbolat.bookaggregator.messaging.RabbitTopology;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;

/**
 * Queues emails to {@code kr.notify.email}; {@code EmailListener} sends them. Inside a transaction the email
 * leaves only after the commit, so that a rolled back registration never sends a link to nobody.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailPublisher {

    private static final String LOCALE = "ru";

    private final MessageSender messageSender;

    public void publish(EmailType type, String to, Map<String, Object> model) {
        EmailMessage message = new EmailMessage(UUID.randomUUID(), type, to, LOCALE, model);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(message);
                }
            });
        } else {
            send(message);
        }
    }

    private void send(EmailMessage message) {
        if (messageSender.send(RabbitTopology.NOTIFY_EMAIL_KEY, message.messageId(), message)) {
            log.info("Email {} to {} queued, messageId={}", message.type(), Emails.mask(message.to()), message.messageId());
        }
    }
}
