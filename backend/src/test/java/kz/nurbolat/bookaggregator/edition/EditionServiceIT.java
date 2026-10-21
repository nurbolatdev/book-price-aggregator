package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse.OfferInfo;
import kz.nurbolat.bookaggregator.edition.dto.PriceVerdict.Label;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.editionWithHistory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@Sql("classpath:db/dev/catalog-seed.sql")
class EditionServiceIT extends AbstractIntegrationTest {

    @Autowired
    private EditionService editionService;

    @Autowired
    private EditionRepository editionRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldReturnCardWithOffersAndOtherEditions() {
        Long id = editionRepository.findByIsbn13("9785601000017").orElseThrow().getId();

        EditionDetailResponse card = editionService.getDetail(id);

        assertThat(card.book().title()).isEqualTo("Мастер и Маргарита");
        assertThat(card.publisher()).isEqualTo("АСТ");
        assertThat(card.offers()).isNotEmpty();
        assertThat(card.offers()).isSortedAccordingTo(Comparator
                .comparing(OfferInfo::inStock).reversed()
                .thenComparing(OfferInfo::price));
        assertThat(card.offers().get(0).storeName()).isNotBlank();
        assertThat(card.bestPrice()).isNotNull();
        assertThat(card.bestPrice().price()).isEqualByComparingTo(card.offers().get(0).price());
        assertThat(card.otherEditions()).extracting(EditionDetailResponse.OtherEdition::label)
                .containsExactlyInAnyOrder("Азбука 2022 · мягкий", "Эксмо 2021 · твёрдый");
        assertThat(card.otherEditions()).allSatisfy(other -> assertThat(other.bestPrice()).isNotNull());
    }

    @Test
    void shouldHaveNoBestPriceWhenNothingIsInStock() {
        long id = editionWithHistory(jdbc, "Нет в наличии", false, daysAgo(30, 2000));

        EditionDetailResponse card = editionService.getDetail(id);

        assertThat(card.offers()).hasSize(1);
        assertThat(card.bestPrice()).isNull();
        assertThat(card.priceVerdict().label()).isEqualTo(Label.NOT_ENOUGH_DATA);
    }

    @Test
    void shouldSayBelowAverageWhenPriceDroppedRecently() {
        long id = editionWithHistory(jdbc, "Подешевела", true, daysAgo(60, 5000), daysAgo(2, 4000));

        var verdict = editionService.getDetail(id).priceVerdict();

        assertThat(verdict.label()).isEqualTo(Label.BELOW_AVERAGE);
        assertThat(verdict.diffPercent()).isNegative();
        assertThat(verdict.avg90d()).isBetween(new java.math.BigDecimal("4800"), new java.math.BigDecimal("5000"));
    }

    @Test
    void shouldSayAboveAverageWhenPriceRoseRecently() {
        long id = editionWithHistory(jdbc, "Подорожала", true, daysAgo(60, 3000), daysAgo(2, 3600));

        var verdict = editionService.getDetail(id).priceVerdict();

        assertThat(verdict.label()).isEqualTo(Label.ABOVE_AVERAGE);
        assertThat(verdict.diffPercent()).isPositive();
    }

    @Test
    void shouldSayAverageWhenPriceIsStable() {
        long id = editionWithHistory(jdbc, "Стабильная", true, daysAgo(40, 2500));

        var verdict = editionService.getDetail(id).priceVerdict();

        assertThat(verdict.label()).isEqualTo(Label.AVERAGE);
        assertThat(verdict.diffPercent()).isZero();
    }

    @Test
    void shouldNotJudgeWhenHistoryIsShorterThanTwoWeeks() {
        long id = editionWithHistory(jdbc, "Новинка", true, daysAgo(10, 3000), daysAgo(1, 2000));

        var verdict = editionService.getDetail(id).priceVerdict();

        assertThat(verdict.label()).isEqualTo(Label.NOT_ENOUGH_DATA);
        assertThat(verdict.diffPercent()).isNull();
    }

    @Test
    void shouldThrowNotFoundWhenEditionIsMissing() {
        assertThatThrownBy(() -> editionService.getDetail(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
    }
}
