package kz.nurbolat.bookaggregator.notification;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kz.nurbolat.bookaggregator.config.AppProperties;
import kz.nurbolat.bookaggregator.metrics.KrMetrics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailSenderTest {

    private static final String BASE_URL = "https://kitapradar.kz";

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailSender sender = new EmailSender(mailSender, templateEngine(),
            new MailProperties("KitapRadar <no-reply@kitapradar.kz>"),
            new AppProperties(BASE_URL, null, null), new KrMetrics(new SimpleMeterRegistry()));

    @Test
    void shouldRenderVerifyEmailWithAbsoluteLinksInHtmlAndText() throws Exception {
        String link = BASE_URL + "/verify-email?token=abc123";

        String raw = send(EmailType.VERIFY_EMAIL, Map.of("name", "Нурболат", "link", link));

        assertThat(raw).contains("multipart/alternative", "text/plain", "text/html");
        assertThat(raw).contains("Здравствуйте, Нурболат!", "Подтвердить email", "href=\"" + link + "\"");
        assertThat(raw).contains("href=\"" + BASE_URL + "\"");
        // The text part repeats the link for clients without HTML
        assertThat(raw.indexOf(link)).isNotEqualTo(raw.lastIndexOf(link));
        assertThat(raw).doesNotContain("href=\"/", "${", "[(");
    }

    @Test
    void shouldRenderResetPasswordWithoutNameWhenUserHasNone() throws Exception {
        String link = BASE_URL + "/reset-password?token=xyz";
        Map<String, Object> model = new java.util.HashMap<>();
        model.put("name", null);
        model.put("link", link);

        String raw = send(EmailType.RESET_PASSWORD, model);

        assertThat(raw).contains("Здравствуйте!", "Задать новый пароль", "href=\"" + link + "\"")
                .doesNotContain("null");
    }

    @Test
    void shouldAddressEmailFromConfiguredSender() throws Exception {
        MimeMessage message = sendMessage(EmailType.VERIFY_EMAIL,
                Map.of("name", "A", "link", BASE_URL + "/verify-email?token=t"));

        assertThat(message.getFrom()).containsExactly(new InternetAddress("KitapRadar <no-reply@kitapradar.kz>"));
        assertThat(message.getAllRecipients()).containsExactly(new InternetAddress("reader@example.com"));
        assertThat(message.getSubject()).isEqualTo("Подтвердите email — KitapRadar");
    }

    private String send(EmailType type, Map<String, Object> model) throws Exception {
        MimeMessage message = sendMessage(type, model);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return decodeParts(out.toString(StandardCharsets.UTF_8));
    }

    private MimeMessage sendMessage(EmailType type, Map<String, Object> model) throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        sender.send(new EmailMessage(UUID.randomUUID(), type, "reader@example.com", "ru", null, model, List.of()));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();
        message.saveChanges();
        return message;
    }

    /** Parts are quoted-printable or base64; tests read the decoded text. */
    private static String decodeParts(String raw) throws Exception {
        MimeMessage parsed = new MimeMessage(Session.getInstance(new Properties()),
                new java.io.ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8)));
        StringBuilder text = new StringBuilder(raw.substring(0, raw.indexOf("\r\n\r\n")));
        collect(parsed.getContent(), text);
        return text.toString();
    }

    private static void collect(Object content, StringBuilder text) throws Exception {
        if (content instanceof jakarta.mail.Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                text.append('\n').append(multipart.getBodyPart(i).getContentType());
                collect(multipart.getBodyPart(i).getContent(), text);
            }
        } else {
            text.append('\n').append(content);
        }
    }

    /** Same resolvers as the application: text versions first, HTML for every other name. */
    private static SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver html = new ClassLoaderTemplateResolver();
        html.setPrefix("templates/");
        html.setSuffix(".html");
        html.setTemplateMode(TemplateMode.HTML);
        html.setCharacterEncoding("UTF-8");
        html.setOrder(1);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.addTemplateResolver(new MailTemplateConfig().textEmailTemplateResolver());
        engine.addTemplateResolver(html);
        return engine;
    }
}
