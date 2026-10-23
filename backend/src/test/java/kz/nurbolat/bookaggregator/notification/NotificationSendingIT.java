package kz.nurbolat.bookaggregator.notification;

import kz.nurbolat.bookaggregator.support.AbstractMailIT;
import kz.nurbolat.bookaggregator.support.TestData;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Copies of one notification email: processed at the same moment, or arriving after the notification was closed.
 * The listener is called directly from test threads, so the timing is controlled, not left to the broker.
 */
class NotificationSendingIT extends AbstractMailIT {

    @Autowired
    private EmailListener listener;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @SpyBean
    private EmailSender emailSender;

    @Test
    void shouldSendOnceWhenTwoCopiesAreProcessedAtTheSameMoment() throws Exception {
        User user = user();
        long notification = notification(user, "QUEUED", null);
        CountDownLatch firstIsSending = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (first.getAndSet(false)) {
                firstIsSending.countDown();
                // The second copy checks the notification while this one is still at the mail server
                Thread.sleep(1500);
            }
            return invocation.callRealMethod();
        }).when(emailSender).send(any());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> one = pool.submit(() -> listener.onEmail(copy(user, notification)));
            assertThat(firstIsSending.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> two = pool.submit(() -> listener.onEmail(copy(user, notification)));
            one.get(20, TimeUnit.SECONDS);
            two.get(20, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        verify(emailSender, times(1)).send(any());
        assertThat(awaitMail(user.getEmail(), 1)).hasSize(1);
        assertThat(status(notification)).isEqualTo("SENT");
    }

    @Test
    void shouldNotSendLateCopyWhenNotificationWasClosedForGood() throws Exception {
        User user = user();
        long notPublished = notification(user, "FAILED", NotificationErrors.NOT_PUBLISHED);
        long inactive = notification(user, "FAILED", NotificationErrors.BEACON_INACTIVE);

        listener.onEmail(copy(user, notPublished));
        listener.onEmail(copy(user, inactive));

        Thread.sleep(1000);
        assertThat(inbox(user.getEmail())).isEmpty();
        assertThat(jdbc.queryForList("SELECT error FROM notifications WHERE id IN (?, ?) ORDER BY id", String.class,
                notPublished, inactive)).containsExactly("NOT_PUBLISHED", "BEACON_INACTIVE");
    }

    @Test
    void shouldSendCopyWhenEarlierAttemptFailedAtTheMailServer() throws Exception {
        User user = user();
        long notification = notification(user, "FAILED", "Mail server connection failed");

        listener.onEmail(copy(user, notification));

        assertThat(awaitMail(user.getEmail(), 1)).hasSize(1);
        assertThat(status(notification)).isEqualTo("SENT");
    }

    private EmailMessage copy(User user, long notification) {
        // A fresh message id each time, as the relay gives every publication
        return new EmailMessage(UUID.randomUUID(), EmailType.VERIFY_EMAIL, user.getEmail(), "ru", null,
                Map.of("name", "Тест", "link", "http://localhost:3000/verify-email?token=t"), List.of(notification));
    }

    private long notification(User user, String status, String error) {
        return jdbc.queryForObject("""
                INSERT INTO notifications (user_id, status, delivery, payload, error, published_at)
                VALUES (?, ?, 'INSTANT', '{}'::jsonb, ?, NOW()) RETURNING id""", Long.class, user.getId(), status, error);
    }

    private String status(long notification) {
        return jdbc.queryForObject("SELECT status FROM notifications WHERE id = ?", String.class, notification);
    }

    private User user() {
        User user = TestData.user();
        user.setEmailVerified(true);
        return userRepository.save(user);
    }
}
