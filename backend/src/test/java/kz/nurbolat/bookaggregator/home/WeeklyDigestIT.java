package kz.nurbolat.bookaggregator.home;

import kz.nurbolat.bookaggregator.beacon.BeaconEmailRelay;
import kz.nurbolat.bookaggregator.support.AbstractMailIT;
import kz.nurbolat.bookaggregator.support.TestData;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.function.BooleanSupplier;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

class WeeklyDigestIT extends AbstractMailIT {

    @Autowired
    private WeeklyDigestService weeklyDigestService;

    @Autowired
    private BeaconEmailRelay relay;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @SpyBean
    private DropsService dropsService;

    @Test
    void shouldQueueDigestOnlyForVerifiedUsersWithTheSetting() throws Exception {
        editionWithHistory(jdbc, "Подешевела на неделе", true, daysAgo(30, 4000), daysAgo(1, 2000));
        User recipient = user(true, true);
        User unverified = user(false, true);
        User optedOut = user(true, false);

        weeklyDigestService.sendWeeklyDigests();

        assertThat(awaitMail(recipient.getEmail(), 1)).hasSize(1);
        assertThat(awaitMail(recipient.getEmail(), 1).get(0).getSubject()).startsWith("Подборка недели: ");
        await(() -> "SENT".equals(jdbc.queryForObject(
                "SELECT status FROM notifications WHERE user_id = ?", String.class, recipient.getId())));
        assertThat(jdbc.queryForMap("SELECT delivery, beacon_id, payload ->> 'isoWeek' AS iso_week, published_at "
                + "FROM notifications WHERE user_id = ?", recipient.getId()))
                .containsEntry("delivery", "WEEKLY").containsEntry("beacon_id", null)
                .satisfies(row -> {
                    assertThat((String) row.get("iso_week")).matches("\\d{4}-W\\d{2}");
                    assertThat(row.get("published_at")).isNotNull();
                });
        assertThat(count(unverified)).isZero();
        assertThat(count(optedOut)).isZero();
        assertThat(inbox(unverified.getEmail())).isEmpty();
        assertThat(inbox(optedOut.getEmail())).isEmpty();
    }

    @Test
    void shouldSendOnlyOnceInAWeek() throws Exception {
        editionWithHistory(jdbc, "Подешевела на неделе", true, daysAgo(30, 4000), daysAgo(1, 2000));
        User recipient = user(true, true);

        weeklyDigestService.sendWeeklyDigests();
        awaitMail(recipient.getEmail(), 1);
        weeklyDigestService.sendWeeklyDigests();

        Thread.sleep(500);
        assertThat(count(recipient)).isEqualTo(1);
        assertThat(inbox(recipient.getEmail())).hasSize(1);
    }

    @Test
    void shouldSendNothingWhenNothingGotCheaper() throws Exception {
        User recipient = user(true, true);
        doReturn(List.of()).when(dropsService).getDrops(8);

        assertThat(weeklyDigestService.sendWeeklyDigests()).isZero();

        assertThat(count(recipient)).isZero();
        assertThat(inbox(recipient.getEmail())).isEmpty();
    }

    @Test
    void shouldPublishAgainWeeklyDigestThatNeverReachedTheBroker() throws Exception {
        User recipient = user(true, true);
        long id = jdbc.queryForObject("""
                INSERT INTO notifications (user_id, delivery, payload, created_at)
                VALUES (?, 'WEEKLY', '{"isoWeek": "2026-W44", "periodText": "19–26 октября", "items": [
                  {"editionId": 1, "title": "Шантарам", "author": "Грегори Дэвид Робертс", "storeName": "Flip.kz",
                   "price": 4000, "previousPrice": 5000, "dropPercent": 20}]}'::jsonb,
                        NOW() - INTERVAL '2 minutes')
                RETURNING id""", Long.class, recipient.getId());

        assertThat(relay.run().republished()).isEqualTo(1);

        assertThat(awaitMail(recipient.getEmail(), 1)).hasSize(1);
        assertThat(awaitMail(recipient.getEmail(), 1).get(0).getSubject()).isEqualTo("Подборка недели: 1 книга подешевела до −20%");
        await(() -> "SENT".equals(jdbc.queryForObject("SELECT status FROM notifications WHERE id = ?", String.class, id)));
        assertThat(relay.run().republished()).isZero();
    }

    private int count(User user) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND delivery = 'WEEKLY'",
                Integer.class, user.getId());
    }

    private User user(boolean verified, boolean weeklyDigest) {
        User user = TestData.user();
        user.setEmailVerified(verified);
        user.setWeeklyDigest(weeklyDigest);
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
