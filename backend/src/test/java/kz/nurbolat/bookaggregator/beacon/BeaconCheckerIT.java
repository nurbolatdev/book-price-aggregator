package kz.nurbolat.bookaggregator.beacon;

import jakarta.mail.internet.MimeMessage;
import kz.nurbolat.bookaggregator.messaging.RabbitTopology;
import kz.nurbolat.bookaggregator.messaging.message.PriceChangedMessage;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.support.AbstractMailIT;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole path: a price change message → {@code BeaconChecker} → notification → email through the queue → SMTP.
 */
class BeaconCheckerIT extends AbstractMailIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldEmailOnceForEachNewLowWhenPriceDrops() throws Exception {
        User user = user(true);
        long edition = editionWithHistory(jdbc, "Мастер и Маргарита", true, daysAgo(3, 3610));
        long beacon = beacon(user, edition, "ANY_DROP", null, "3610");

        priceChanged(edition, "3150");
        List<MimeMessage> mail = awaitMail(user.getEmail(), 1);

        assertThat(mail).hasSize(1);
        assertThat(mail.get(0).getSubject()).isEqualTo("Цена упала: «Мастер и Маргарита» — 3 150 ₸");
        assertThat(mail.get(0).getHeader("List-Unsubscribe")[0]).startsWith("<http://localhost:3000/unsubscribe?token=");
        assertThat(plainText(mail.get(0))).contains("Было 3 610 ₸ → стало 3 150 ₸", "Flip.kz");
        Map<String, Object> state = jdbc.queryForMap(
                "SELECT status, last_notified_price, last_triggered_at FROM beacons WHERE id = ?", beacon);
        assertThat(state.get("status")).isEqualTo("TRIGGERED");
        assertThat((BigDecimal) state.get("last_notified_price")).isEqualByComparingTo("3150");
        assertThat(state.get("last_triggered_at")).isNotNull();
        await(() -> "SENT".equals(notificationStatus(beacon)));

        // The same price again: nothing new to report
        priceChanged(edition, "3150");
        Thread.sleep(1500);
        assertThat(inbox(user.getEmail())).hasSize(1);

        // Lower still: a second email from the last reported price
        priceChanged(edition, "2990");
        mail = awaitMail(user.getEmail(), 2);
        assertThat(mail).hasSize(2);
        assertThat(plainText(mail.get(1))).contains("Было 3 150 ₸ → стало 2 990 ₸");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE beacon_id = ?", Integer.class, beacon))
                .isEqualTo(2);
    }

    @Test
    void shouldTriggerWithoutEmailWhenAddressIsNotConfirmed() throws Exception {
        User user = user(false);
        long edition = editionWithHistory(jdbc, "Дюна", true, daysAgo(3, 5000));
        long beacon = beacon(user, edition, "BELOW_PRICE", "4500", "5000");

        priceChanged(edition, "4400");
        await(() -> notificationStatus(beacon) != null);

        assertThat(notificationStatus(beacon)).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("SELECT error FROM notifications WHERE beacon_id = ?", String.class, beacon))
                .isEqualTo("EMAIL_NOT_VERIFIED");
        assertThat(jdbc.queryForObject("SELECT status FROM beacons WHERE id = ?", String.class, beacon)).isEqualTo("TRIGGERED");
        Thread.sleep(1000);
        assertThat(inbox(user.getEmail())).isEmpty();
    }

    @Test
    void shouldLeavePausedBeaconsAndUnmetConditionsAlone() throws Exception {
        User user = user(true);
        long edition = editionWithHistory(jdbc, "Шантарам", true, daysAgo(3, 4000));
        long paused = beacon(user, edition, "ANY_DROP", null, "4000");
        jdbc.update("UPDATE beacons SET status = 'PAUSED' WHERE id = ?", paused);
        User other = user(true);
        long unmet = beacon(other, edition, "DISCOUNT_PERCENT", null, "4000");
        jdbc.update("UPDATE beacons SET target_percent = 20 WHERE id = ?", unmet);

        // 10% off: the 20% beacon does not fire, the paused one is not checked
        priceChanged(edition, "3600");
        Thread.sleep(1500);

        assertThat(jdbc.queryForList("SELECT status FROM beacons WHERE id IN (?, ?) ORDER BY id", String.class, paused, unmet))
                .containsExactly("PAUSED", "ACTIVE");
        assertThat(inbox(user.getEmail())).isEmpty();
        assertThat(inbox(other.getEmail())).isEmpty();
    }

    private void priceChanged(long edition, String price) {
        jdbc.update("UPDATE offers SET price = ?, last_seen_at = NOW() WHERE edition_id = ?", new BigDecimal(price), edition);
        rabbitTemplate.convertAndSend("kr.events", RabbitTopology.PRICE_CHANGED_KEY, new PriceChangedMessage(
                UUID.randomUUID(), null, edition, StoreCode.FLIP, null, new BigDecimal(price), true, Instant.now()));
    }

    private long beacon(User user, long edition, String type, String target, String baseline) {
        return jdbc.queryForObject("""
                INSERT INTO beacons (user_id, edition_id, condition_type, target_price, target_percent, baseline_price)
                VALUES (?, ?, ?, ?, CASE WHEN ? = 'DISCOUNT_PERCENT' THEN 10 END, ?) RETURNING id""", Long.class,
                user.getId(), edition, type, target == null ? null : new BigDecimal(target), type, new BigDecimal(baseline));
    }

    private String notificationStatus(long beacon) {
        return jdbc.query("SELECT status FROM notifications WHERE beacon_id = ? ORDER BY id DESC LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : null, beacon);
    }

    private User user(boolean verified) {
        User user = kz.nurbolat.bookaggregator.support.TestData.user();
        user.setEmailVerified(verified);
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
}
