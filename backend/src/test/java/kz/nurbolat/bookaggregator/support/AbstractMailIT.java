package kz.nurbolat.bookaggregator.support;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import kz.nurbolat.bookaggregator.messaging.MessagingProperties;
import kz.nurbolat.bookaggregator.messaging.RabbitTopology;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Email flow tests: queue consumers are started, so that emails really go through {@code kr.notify.email} to an
 * SMTP server inside the test (GreenMail). The crawler is a mock and the context is closed after each class,
 * like in the listener tests.
 */
@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=true",
        "spring.mail.host=localhost",
        "spring.mail.port=3025"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractMailIT extends AbstractIntegrationTest {

    private static final long MAIL_WAIT_MS = 10_000;

    @RegisterExtension
    protected static final GreenMailExtension GREEN_MAIL = new GreenMailExtension(ServerSetupTest.SMTP);

    @MockBean
    protected StoreCrawlService crawlService;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private MessagingProperties messagingProperties;

    @BeforeEach
    void emptyEmailQueues() {
        rabbitAdmin.purgeQueue(RabbitTopology.NOTIFY_EMAIL_QUEUE);
        rabbitAdmin.purgeQueue(messagingProperties.deadLetterQueue());
    }

    /**
     * Waits for emails to the address.
     */
    protected List<MimeMessage> awaitMail(String to, int count) throws InterruptedException {
        long deadline = System.currentTimeMillis() + MAIL_WAIT_MS;
        List<MimeMessage> received = inbox(to);
        while (received.size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            received = inbox(to);
        }
        return received;
    }

    protected List<MimeMessage> inbox(String to) {
        return Arrays.stream(GREEN_MAIL.getReceivedMessages())
                .filter(message -> {
                    try {
                        return Arrays.stream(message.getAllRecipients()).anyMatch(a -> a.toString().equals(to));
                    } catch (jakarta.mail.MessagingException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .toList();
    }

    /**
     * @return the token of the first link {@code <path>?token=...} in the plain-text part of the email
     */
    protected static String linkToken(MimeMessage message, String link) throws Exception {
        Matcher matcher = Pattern.compile(Pattern.quote(link) + "\\?token=([A-Za-z0-9_-]+)").matcher(plainText(message));
        if (!matcher.find()) {
            throw new AssertionError("No link " + link + " in the email:\n" + plainText(message));
        }
        return matcher.group(1);
    }

    protected static String plainText(MimeMessage message) throws Exception {
        Multipart multipart = (Multipart) message.getContent();
        return findText(multipart);
    }

    private static String findText(Multipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/plain")) {
                return (String) part.getContent();
            }
            if (part.getContent() instanceof Multipart nested) {
                String text = findText(nested);
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }

    protected ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
