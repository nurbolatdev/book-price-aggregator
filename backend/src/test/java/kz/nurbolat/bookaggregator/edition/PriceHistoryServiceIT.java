package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.edition.dto.PriceHistoryResponse;
import kz.nurbolat.bookaggregator.edition.dto.PriceHistoryResponse.Point;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.addOffer;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PriceHistoryServiceIT extends AbstractIntegrationTest {

    @Autowired
    private PriceHistoryService priceHistoryService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldCarryLastPriceForwardOnDaysWithoutChanges() {
        long editionId = editionWithHistory(jdbc, "История", true, daysAgo(10, 3000), daysAgo(4, 2500));

        PriceHistoryResponse history = priceHistoryService.getHistory(editionId, PricePeriod.ONE_MONTH);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assertThat(history.points()).hasSize(11);
        assertThat(history.points().get(0)).isEqualTo(new Point(today.minusDays(10), price(3000)));
        assertThat(priceOn(history.points(), today.minusDays(5))).isEqualByComparingTo("3000");
        assertThat(priceOn(history.points(), today.minusDays(4))).isEqualByComparingTo("2500");
        assertThat(priceOn(history.points(), today)).isEqualByComparingTo("2500");
    }

    @Test
    void shouldTakeLowestPriceAmongStoresForEachDay() {
        long editionId = editionWithHistory(jdbc, "Два магазина", true, daysAgo(6, 3000));
        addOffer(jdbc, editionId, "MELOMAN", true, daysAgo(6, 3200), daysAgo(3, 2800));

        PriceHistoryResponse history = priceHistoryService.getHistory(editionId, PricePeriod.ONE_MONTH);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assertThat(priceOn(history.points(), today.minusDays(4))).isEqualByComparingTo("3000");
        assertThat(priceOn(history.points(), today.minusDays(3))).isEqualByComparingTo("2800");
        assertThat(history.min()).isEqualByComparingTo("2800");
        assertThat(history.max()).isEqualByComparingTo("3000");
        // 3 days at 3000 (6..4 days ago) and 4 days at 2800 (3 days ago..today)
        assertThat(history.avg()).isEqualByComparingTo("2885.71");
    }

    @Test
    void shouldCutHistoryToThePeriod() {
        long editionId = editionWithHistory(jdbc, "Старая", true, daysAgo(120, 4000), daysAgo(50, 3500));

        PriceHistoryResponse month = priceHistoryService.getHistory(editionId, PricePeriod.ONE_MONTH);
        PriceHistoryResponse halfYear = priceHistoryService.getHistory(editionId, PricePeriod.SIX_MONTHS);

        assertThat(month.points()).hasSize(31).allSatisfy(p -> assertThat(p.price()).isEqualByComparingTo("3500"));
        assertThat(halfYear.points()).hasSize(121);
        assertThat(halfYear.max()).isEqualByComparingTo("4000");
    }

    @Test
    void shouldReturnEmptyHistoryWhenEditionHasNoOffers() {
        Long bookId = jdbc.queryForObject("INSERT INTO books (title, title_norm) VALUES ('X', 'x') RETURNING id", Long.class);
        Long editionId = jdbc.queryForObject("INSERT INTO editions (book_id) VALUES (?) RETURNING id", Long.class, bookId);

        PriceHistoryResponse history = priceHistoryService.getHistory(editionId, PricePeriod.THREE_MONTHS);

        assertThat(history.points()).isEmpty();
        assertThat(history.min()).isNull();
    }

    private static BigDecimal priceOn(List<Point> points, LocalDate date) {
        return points.stream().filter(p -> p.date().equals(date)).findFirst().orElseThrow().price();
    }

    private static BigDecimal price(int value) {
        return new BigDecimal(value).setScale(2);
    }
}
