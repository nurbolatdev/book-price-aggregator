package kz.nurbolat.bookaggregator.beacon;

import kz.nurbolat.bookaggregator.messaging.MessageSender;
import kz.nurbolat.bookaggregator.messaging.RabbitTopology;
import kz.nurbolat.bookaggregator.notification.EmailPublisher;
import kz.nurbolat.bookaggregator.notification.EmailSender;
import kz.nurbolat.bookaggregator.notification.EmailType;
import kz.nurbolat.bookaggregator.support.AbstractMailIT;
import kz.nurbolat.bookaggregator.support.TestData;
import kz.nurbolat.bookaggregator.user.NotifyFrequency;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * Failure scenarios of beacon email delivery: SMTP errors, a broker that does not confirm, copies published
 * twice, the relay and the digest. Failures are injected with spies, nothing is really switched off.
 */
class BeaconEmailDeliveryIT extends AbstractMailIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BeaconChecker checker;

    @Autowired
    private BeaconEmailRelay relay;

    @Autowired
    private DigestService digestService;

    @Autowired
    private EmailPublisher emailPublisher;

    @SpyBean
    private MessageSender messageSender;

    @SpyBean
    private EmailSender emailSender;

    @Test
    void shouldSendEmailWhenFirstSmtpAttemptFails() throws Exception {
        doThrow(new MailSendException("mail server is down")).doCallRealMethod().when(emailSender).send(any());
        Fired fired = fire(user(NotifyFrequency.INSTANT));

        assertThat(awaitMail(fired.email(), 1)).hasSize(1);
        await(() -> "SENT".equals(status(fired.notification())));
        assertThat(jdbc.queryForObject("SELECT error FROM notifications WHERE id = ?", String.class, fired.notification()))
                .isNull();
    }

    @Test
    void shouldPublishAgainWhenBrokerDidNotConfirmFirstPublication() throws Exception {
        doReturn(false).doCallRealMethod().when(messageSender).sendConfirmed(any(), any(), any(), any());
        Fired fired = fire(user(NotifyFrequency.INSTANT));
        assertThat(publishedAt(fired.notification())).isNull();

        // Still within a minute: the relay leaves it to the first publication
        assertThat(relay.run().republished()).isZero();
        age(fired.notification(), Duration.ofMinutes(2));

        assertThat(relay.run().republished()).isEqualTo(1);
        assertThat(awaitMail(fired.email(), 1)).hasSize(1);
        await(() -> "SENT".equals(status(fired.notification())));
        assertThat(publishedAt(fired.notification())).isNotNull();
        assertThat(relay.run().republished()).isZero();
    }

    @Test
    void shouldNotSendAgainWhenCopyArrivesAfterEmailWasSent() throws Exception {
        Fired fired = fire(user(NotifyFrequency.INSTANT));
        awaitMail(fired.email(), 1);
        await(() -> "SENT".equals(status(fired.notification())));

        // As if the confirmation was lost and the email was published once more
        assertThat(emailPublisher.publishNow(EmailType.PRICE_DROP, fired.email(), "copy",
                Map.of("title", "copy"), List.of(fired.notification()))).isTrue();

        Thread.sleep(1500);
        assertThat(inbox(fired.email())).hasSize(1);
    }

    @Test
    void shouldLeavePublishedRefusedAndDigestNotificationsToOthers() {
        User user = user(NotifyFrequency.INSTANT);
        long inQueue = notification(user, "QUEUED", "INSTANT", true, null, Duration.ofMinutes(30));
        long refused = notification(user, "FAILED", "INSTANT", false, "EMAIL_NOT_VERIFIED", Duration.ofMinutes(30));
        long digest = notification(user, "QUEUED", "DIGEST", false, null, Duration.ofMinutes(30));

        assertThat(relay.run().republished()).isZero();

        assertThat(List.of(status(inQueue), status(refused), status(digest))).containsExactly("QUEUED", "FAILED", "QUEUED");
        assertThat(publishedAt(digest)).isNull();
    }

    @Test
    void shouldGiveUpOnlyOnUnpublishedInstantEmailsAfter24Hours() {
        User user = user(NotifyFrequency.INSTANT);
        long lost = notification(user, "QUEUED", "INSTANT", false, null, Duration.ofHours(25));
        long published = notification(user, "QUEUED", "INSTANT", true, null, Duration.ofHours(25));

        assertThat(relay.run().abandoned()).isEqualTo(1);

        assertThat(status(lost)).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("SELECT error FROM notifications WHERE id = ?", String.class, lost))
                .isEqualTo("NOT_PUBLISHED");
        assertThat(status(published)).isEqualTo("QUEUED");
    }

    @Test
    void shouldDeliverAsChosenWhenBeaconFiredAfterUserChangesFrequency() throws Exception {
        User user = user(NotifyFrequency.DAILY);
        Fired fired = fire(user);
        assertThat(jdbc.queryForObject("SELECT delivery FROM notifications WHERE id = ?", String.class, fired.notification()))
                .isEqualTo("DIGEST");
        jdbc.update("UPDATE users SET notify_frequency = 'INSTANT' WHERE id = ?", user.getId());
        age(fired.notification(), Duration.ofMinutes(5));

        // Not sent as an instant email after the switch...
        assertThat(relay.run().republished()).isZero();
        Thread.sleep(1000);
        assertThat(inbox(fired.email())).isEmpty();

        // ...but in the digest, as when it fired
        digestService.sendDailyDigests();
        assertThat(awaitMail(fired.email(), 1).get(0).getSubject()).startsWith("Сводка цен");
    }

    @Test
    void shouldNotPutDigestTogetherAgainWhenItsMessageIsAlreadyPublished() throws Exception {
        User user = user(NotifyFrequency.DAILY);
        // Published but not sent yet: waiting in the queue or in the dead-letter queue
        notification(user, "QUEUED", "DIGEST", true, null, Duration.ofHours(2));

        digestService.sendDailyDigests();

        Thread.sleep(1000);
        assertThat(inbox(user.getEmail())).isEmpty();
    }

    @Test
    void shouldReportUnroutableMessageAsNotPublished() {
        assertThat(messageSender.sendConfirmed("nowhere.routing.key", UUID.randomUUID(), "lost", Duration.ofSeconds(5)))
                .isFalse();
        assertThat(messageSender.sendConfirmed(RabbitTopology.PRICE_CHANGED_KEY + ".unknown", UUID.randomUUID(), "lost",
                Duration.ofSeconds(5))).isFalse();
    }

    @Test
    void shouldRecordOneNotificationWhenSameDropIsCheckedTwiceAtOnce() throws Exception {
        User user = user(NotifyFrequency.DAILY);
        long edition = editionWithHistory(jdbc, "Дюна", true, daysAgo(3, 5000));
        long beacon = beacon(user, edition);
        jdbc.update("UPDATE offers SET price = 4500 WHERE edition_id = ?", edition);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> check = () -> {
                start.await();
                return checker.check(edition);
            };
            List<Future<Integer>> checks = List.of(pool.submit(check), pool.submit(check));
            start.countDown();
            int fired = 0;
            for (Future<Integer> result : checks) {
                fired += result.get(10, TimeUnit.SECONDS);
            }
            assertThat(fired).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE beacon_id = ?", Integer.class, beacon))
                .isEqualTo(1);
    }

    private Fired fire(User user) {
        long edition = editionWithHistory(jdbc, "Мастер и Маргарита", true, daysAgo(3, 3610));
        long beacon = beacon(user, edition);
        jdbc.update("UPDATE offers SET price = 3150 WHERE edition_id = ?", edition);
        assertThat(checker.check(edition)).isEqualTo(1);
        Long notification = jdbc.queryForObject("SELECT id FROM notifications WHERE beacon_id = ?", Long.class, beacon);
        return new Fired(user.getEmail(), notification);
    }

    private long beacon(User user, long edition) {
        return jdbc.queryForObject("""
                INSERT INTO beacons (user_id, edition_id, condition_type, baseline_price)
                VALUES (?, ?, 'ANY_DROP', 99999) RETURNING id""", Long.class, user.getId(), edition);
    }

    private long notification(User user, String status, String delivery, boolean published, String error, Duration age) {
        long edition = editionWithHistory(jdbc, "Шантарам", true, daysAgo(1, 4000));
        return jdbc.queryForObject("""
                INSERT INTO notifications (user_id, beacon_id, status, delivery, payload, error, created_at, published_at)
                VALUES (?, ?, ?, ?, '{"title": "Шантарам", "newPrice": 4000, "editionId": 1}'::jsonb, ?,
                        NOW() - CAST(? AS INTERVAL), CASE WHEN ? THEN NOW() END)
                RETURNING id""", Long.class, user.getId(), beacon(user, edition), status, delivery, error,
                age.toMinutes() + " minutes", published);
    }

    private void age(long notification, Duration age) {
        jdbc.update("UPDATE notifications SET created_at = NOW() - CAST(? AS INTERVAL) WHERE id = ?",
                age.toMinutes() + " minutes", notification);
    }

    private String status(long notification) {
        return jdbc.queryForObject("SELECT status FROM notifications WHERE id = ?", String.class, notification);
    }

    private Object publishedAt(long notification) {
        return jdbc.queryForObject("SELECT published_at FROM notifications WHERE id = ?", Object.class, notification);
    }

    private User user(NotifyFrequency frequency) {
        User user = TestData.user();
        user.setEmailVerified(true);
        user.setNotifyFrequency(frequency);
        return userRepository.save(user);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("Condition not met in 10 s");
            }
            Thread.sleep(100);
        }
    }

    private record Fired(String email, long notification) {
    }
}
