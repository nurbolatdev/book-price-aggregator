package kz.nurbolat.bookaggregator.home;

import kz.nurbolat.bookaggregator.home.dto.DropItem;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.addOffer;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@Sql("classpath:db/dev/catalog-seed.sql")
class DropsServiceIT extends AbstractIntegrationTest {

    @Autowired
    private DropsService dropsService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void shouldListBiggestWeeklyDropsFirstAndStoreThemInRedis() {
        List<DropItem> drops = dropsService.refresh();

        assertThat(drops).isNotEmpty();
        assertThat(drops).allSatisfy(drop -> {
            assertThat(drop.price()).isLessThan(drop.previousPrice());
            assertThat(drop.dropPercent()).isPositive();
        });
        assertThat(drops).extracting(DropItem::dropPercent).isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(redis.hasKey("home:v2:drops-week")).isTrue();
    }

    @Test
    void shouldIgnorePriceRisesOutOfStockOffersAndOlderDrops() {
        long rose = editionWithHistory(jdbc, "Подорожала", true, daysAgo(30, 2000), daysAgo(2, 2500));
        long outOfStock = editionWithHistory(jdbc, "Нет в наличии", false, daysAgo(30, 3000), daysAgo(2, 1500));
        long oldDrop = editionWithHistory(jdbc, "Давно подешевела", true, daysAgo(30, 3000), daysAgo(20, 2000));
        long fresh = editionWithHistory(jdbc, "Подешевела вчера", true, daysAgo(30, 4000), daysAgo(1, 2000));

        List<Long> ids = dropsService.refresh().stream().map(DropItem::editionId).toList();

        assertThat(ids).contains(fresh).doesNotContain(rose, outOfStock, oldDrop);
        DropItem freshDrop = dropsService.getDrops(50).stream()
                .filter(drop -> drop.editionId() == fresh).findFirst().orElseThrow();
        assertThat(freshDrop.dropPercent()).isEqualTo(50);
    }

    @Test
    void shouldReadFromCacheAndHonourLimit() {
        dropsService.refresh();

        assertThat(dropsService.getDrops(2)).hasSize(2);
    }

    @Test
    void shouldCountInStockStoresOfEdition() {
        long id = editionWithHistory(jdbc, "Две лавки", true, daysAgo(30, 4000), daysAgo(1, 2000));
        addOffer(jdbc, id, "OZON", true, daysAgo(1, 2500));
        addOffer(jdbc, id, "MELOMAN", false, daysAgo(1, 1000));

        DropItem item = dropsService.refresh().stream()
                .filter(drop -> drop.editionId() == id).findFirst().orElseThrow();

        assertThat(item.storesCount()).isEqualTo(2);
    }
}
