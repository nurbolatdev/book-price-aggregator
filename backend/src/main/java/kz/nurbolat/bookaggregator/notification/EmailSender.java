package kz.nurbolat.bookaggregator.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import kz.nurbolat.bookaggregator.common.text.Emails;
import kz.nurbolat.bookaggregator.config.AppProperties;
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

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final MailProperties mailProperties;
    private final AppProperties appProperties;

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
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not build email " + message.messageId(), e);
        }
        mailSender.send(mime);
        log.info("Email {} sent to {}, messageId={}", message.type(), Emails.mask(message.to()), message.messageId());
    }
}
