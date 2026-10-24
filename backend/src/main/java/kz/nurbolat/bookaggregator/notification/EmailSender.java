package kz.nurbolat.bookaggregator.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import kz.nurbolat.bookaggregator.common.text.Emails;
import kz.nurbolat.bookaggregator.config.AppProperties;
import kz.nurbolat.bookaggregator.metrics.KrMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Renders an email from its HTML and text templates and sends it over SMTP. The templates get the message
 * model plus {@code baseUrl}, so every link in them is absolute.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    /** Model key of the link that stops these emails; it becomes the {@code List-Unsubscribe} header. */
    public static final String UNSUBSCRIBE_URL = "unsubscribeUrl";

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final MailProperties mailProperties;
    private final AppProperties appProperties;
    private final KrMetrics metrics;

    public void send(EmailMessage message) {
        Context context = new Context(Locale.forLanguageTag(message.locale()));
        context.setVariables(message.model());
        context.setVariable("baseUrl", appProperties.baseUrl());

        String template = "email/" + message.type().getTemplate();
        String html = templateEngine.process(template, context);
        String text = templateEngine.process(template + ".txt", context);

        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.from());
            helper.setTo(message.to());
            helper.setSubject(message.subjectOrDefault());
            helper.setText(text, html);
            if (message.model().get(UNSUBSCRIBE_URL) instanceof String unsubscribe) {
                // Mail clients show their own "unsubscribe" button for it
                mime.setHeader("List-Unsubscribe", "<" + unsubscribe + ">");
            }
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not build email " + message.messageId(), e);
        }
        try {
            mailSender.send(mime);
        } catch (RuntimeException e) {
            metrics.mail(message.type().name(), "failed");
            throw e;
        }
        metrics.mail(message.type().name(), "sent");
        log.info("Email {} sent to {}, messageId={}", message.type(), Emails.mask(message.to()), message.messageId());
    }
}
