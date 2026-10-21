package kz.nurbolat.bookaggregator.beacon;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.book.BookRepository;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static kz.nurbolat.bookaggregator.support.TestData.book;
import static kz.nurbolat.bookaggregator.support.TestData.edition;
import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class BeaconRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private BeaconRepository beaconRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Edition edition;

    @BeforeEach
    void setUp() {
        user = userRepository.save(user());
        Book book = book("Мастер и Маргарита", "Булгаков");
        book.addEdition(edition(null));
        bookRepository.saveAndFlush(book);
        edition = book.getEditions().get(0);
    }

    @Test
    void shouldSaveBelowPriceBeaconWhenTargetPriceIsSet() {
        Beacon beacon = beacon(BeaconCondition.BELOW_PRICE);
        beacon.setTargetPrice(new BigDecimal("3200.00"));

        beaconRepository.saveAndFlush(beacon);

        assertThat(beaconRepository.findByEditionIdAndStatus(edition.getId(), BeaconStatus.ACTIVE))
                .extracting(Beacon::getId)
                .containsExactly(beacon.getId());
    }

    @Test
    void shouldRejectBelowPriceBeaconWhenTargetPriceIsMissing() {
        Beacon beacon = beacon(BeaconCondition.BELOW_PRICE);

        assertThatThrownBy(() -> beaconRepository.saveAndFlush(beacon))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectDiscountBeaconWhenPercentIsOutOfRange() {
        Beacon beacon = beacon(BeaconCondition.DISCOUNT_PERCENT);
        beacon.setTargetPercent((short) 95);

        assertThatThrownBy(() -> beaconRepository.saveAndFlush(beacon))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldSaveAnyDropBeaconWithoutTargets() {
        Beacon beacon = beacon(BeaconCondition.ANY_DROP);
        beacon.setBaselinePrice(new BigDecimal("3610.00"));

        beaconRepository.saveAndFlush(beacon);

        assertThat(beacon.getId()).isNotNull();
        assertThat(beacon.getStatus()).isEqualTo(BeaconStatus.ACTIVE);
    }

    @Test
    void shouldRejectSecondBeaconWhenUserAlreadyWatchesEdition() {
        beaconRepository.saveAndFlush(beacon(BeaconCondition.ANY_DROP));

        assertThatThrownBy(() -> beaconRepository.saveAndFlush(beacon(BeaconCondition.ANY_DROP)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Beacon beacon(BeaconCondition condition) {
        Beacon beacon = new Beacon();
        beacon.setUser(user);
        beacon.setEdition(edition);
        beacon.setConditionType(condition);
        return beacon;
    }
}
