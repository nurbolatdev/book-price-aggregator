package kz.nurbolat.bookaggregator.notification;

import kz.nurbolat.bookaggregator.common.text.Emails;
import kz.nurbolat.bookaggregator.messaging.MessageSender;
import kz.nurbolat.bookaggregator.messaging.RabbitTopology;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Queues emails to {@code kr.notify.email}; {@code EmailListener} sends them. Inside a transaction the email
 * leaves only after the commit, so that a rolled back registration never sends a link to nobody.
 * <p>
 * Beacon emails (with notification ids) wait for the broker's confirmation and then record {@code published_at};
 * an email that did not reach the broker keeps it empty and is published again by {@code BeaconEmailRelay}.
 * Waiting is bounded by {@link #CONFIRM_TIMEOUT}, far below the relay's age threshold, so the relay never
 * republishes an email whose first publication is still in progress.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailPublisher {

    public static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(5);
    private static final String LOCALE = "ru";

    private final MessageSender messageSender;
    private final NotificationService notificationService;

    public void publish(EmailType type, String to, Map<String, Object> model) {
        publish(type, to, null, model, List.of());
    }

    /**
     * @param subject         {@code null} for the fixed subject of the type
     * @param notificationIds notifications this email delivers; the listener marks them sent or failed
     */
    public void publish(EmailType type, String to, String subject, Map<String, Object> model, List<Long> notificationIds) {
        EmailMessage message = new EmailMessage(UUID.randomUUID(), type, to, LOCALE, subject, model, notificationIds);
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

    /**
     * Publishes right away and waits for the broker, for callers that record the result themselves (the relay
     * holds the notification rows locked).
     *
     * @return {@code true} if the broker confirmed the message
     */
    public boolean publishNow(EmailType type, String to, String subject, Map<String, Object> model,
                              List<Long> notificationIds) {
        EmailMessage message = new EmailMessage(UUID.randomUUID(), type, to, LOCALE, subject, model, notificationIds);
        boolean published = messageSender.sendConfirmed(RabbitTopology.NOTIFY_EMAIL_KEY, message.messageId(), message,
                CONFIRM_TIMEOUT);
        if (published) {
            log.info("Email {} to {} queued, messageId={}", message.type(), Emails.mask(message.to()), message.messageId());
        }
        return published;
    }

    private void send(EmailMessage message) {
        if (message.notificationIds().isEmpty()) {
            // Account emails: if the broker is down the user asks for the link again
            if (messageSender.send(RabbitTopology.NOTIFY_EMAIL_KEY, message.messageId(), message)) {
                log.info("Email {} to {} queued, messageId={}", message.type(), Emails.mask(message.to()), message.messageId());
            }
            return;
        }
        if (messageSender.sendConfirmed(RabbitTopology.NOTIFY_EMAIL_KEY, message.messageId(), message, CONFIRM_TIMEOUT)) {
            notificationService.markPublished(message.notificationIds());
            log.info("Email {} to {} queued, messageId={}", message.type(), Emails.mask(message.to()), message.messageId());
        } else {
            log.warn("Email {} for notifications {} not published; the relay will try again",
                    message.type(), message.notificationIds());
        }
    }
}
