package kz.nurbolat.bookaggregator.notification;

import kz.nurbolat.bookaggregator.messaging.ProcessedMessages;
import kz.nurbolat.bookaggregator.messaging.RabbitTopology;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumer of {@code kr.notify.email}. An SMTP failure is retried and finally dead-lettered (rules of M17);
 * a redelivered message that was already sent is skipped. The notifications the email delivers are marked
 * {@code SENT}, or {@code FAILED} with the error after each failed attempt (a later successful retry marks them sent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
class EmailListener {

    private final EmailSender emailSender;
    private final ProcessedMessages processedMessages;
    private final NotificationService notificationService;

    @RabbitListener(id = "notify-email", queues = RabbitTopology.NOTIFY_EMAIL_QUEUE, concurrency = "2")
    void onEmail(EmailMessage message) {
        MDC.put("traceId", message.messageId().toString());
        try {
            if (!processedMessages.runOnce(ProcessedMessages.EMAIL, message.messageId(), () -> send(message))) {
                log.info("Email message {} already sent, skipped", message.messageId());
            }
        } finally {
            MDC.remove("traceId");
        }
    }

    private void send(EmailMessage message) {
        List<Long> notifications = message.notificationIds() == null ? List.of() : message.notificationIds();
        try {
            emailSender.send(message);
        } catch (RuntimeException e) {
            notificationService.markFailed(notifications, e.getMessage());
            throw e;
        }
        notificationService.markSent(notifications);
    }
}
