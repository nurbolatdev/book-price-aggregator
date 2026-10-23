package kz.nurbolat.bookaggregator.beacon;

import jakarta.mail.internet.MimeMessage;
import kz.nurbolat.bookaggregator.scheduler.DigestScheduler;
import kz.nurbolat.bookaggregator.support.AbstractMailIT;
import kz.nurbolat.bookaggregator.user.NotifyFrequency;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;

class DigestSchedulerIT extends AbstractMailIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BeaconChecker checker;

    @Autowired
    private DigestScheduler digestScheduler;

    @Test
    void shouldSendOneDigestWhenThreeBeaconsOfDailyUserFired() throws Exception {
        User user = user();
        user.setEmailVerified(true);
        user.setNotifyFrequency(NotifyFrequency.DAILY);
        userRepository.save(user);
        List<String> titles = List.of("Дюна", "Шантарам", "Мастер и Маргарита");
        for (String title : titles) {
            long edition = editionWithHistory(jdbc, title, true, daysAgo(3, 4000));
            jdbc.update("""
                    INSERT INTO beacons (user_id, edition_id, condition_type, baseline_price)
                    VALUES (?, ?, 'ANY_DROP', 4000)""", user.getId(), edition);
            jdbc.update("UPDATE offers SET price = 3500 WHERE edition_id = ?", edition);
            assertThat(checker.check(edition)).isEqualTo(1);
        }
        // Nothing is sent right away for DAILY
        Thread.sleep(1000);
        assertThat(inbox(user.getEmail())).isEmpty();

        digestScheduler.sendDailyDigests();

        List<MimeMessage> mail = awaitMail(user.getEmail(), 1);
        assertThat(mail).hasSize(1);
        assertThat(mail.get(0).getSubject()).isEqualTo("Сводка цен: подешевели 3 книги — KitapRadar");
        assertThat(plainText(mail.get(0))).contains(titles.toArray(String[]::new))
                .contains("Было 4 000 ₸ → стало 3 500 ₸ (−12%)", "Отключить маячок: http://localhost:3000/unsubscribe?token=");
        awaitSent(user, 3);

        // Already sent: the next run has nothing for this user
        digestScheduler.sendDailyDigests();
        Thread.sleep(1500);
        assertThat(inbox(user.getEmail())).hasSize(1);
    }

    private void awaitSent(User user, int count) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (sent(user) < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        assertThat(sent(user)).isEqualTo(count);
    }

    private int sent(User user) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND status = 'SENT' AND sent_at IS NOT NULL",
                Integer.class, user.getId());
        return count == null ? 0 : count;
    }
}
