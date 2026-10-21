package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.edition.dto.PriceVerdict;
import kz.nurbolat.bookaggregator.edition.dto.PriceVerdict.Label;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PriceVerdictCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    private final PriceVerdictCalculator calculator = new PriceVerdictCalculator();

    @Test
    void shouldBeBelowAverageWhenCurrentPriceIsFivePercentLower() {
        PriceVerdict verdict = calculator.calculate(flat(30, 1000), new BigDecimal("950"), TODAY);

        assertThat(verdict.label()).isEqualTo(Label.BELOW_AVERAGE);
        assertThat(verdict.diffPercent()).isEqualTo(-5);
        assertThat(verdict.avg90d()).isEqualByComparingTo("1000");
    }

    @Test
    void shouldBeAboveAverageWhenCurrentPriceIsFivePercentHigher() {
        PriceVerdict verdict = calculator.calculate(flat(30, 1000), new BigDecimal("1050"), TODAY);

        assertThat(verdict.label()).isEqualTo(Label.ABOVE_AVERAGE);
        assertThat(verdict.diffPercent()).isEqualTo(5);
    }

    @Test
    void shouldBeAverageWithinFivePercent() {
        PriceVerdict verdict = calculator.calculate(flat(30, 1000), new BigDecimal("1040"), TODAY);

        assertThat(verdict.label()).isEqualTo(Label.AVERAGE);
        assertThat(verdict.diffPercent()).isEqualTo(4);
    }

    @Test
    void shouldNotJudgeWhenHistoryIsShorterThanFourteenDays() {
        PriceVerdict verdict = calculator.calculate(flat(13, 1000), new BigDecimal("500"), TODAY);

        assertThat(verdict.label()).isEqualTo(Label.NOT_ENOUGH_DATA);
        assertThat(verdict.avg90d()).isEqualByComparingTo("1000");
    }

    @Test
    void shouldNotJudgeWhenThereIsNoHistoryOrNoCurrentPrice() {
        assertThat(calculator.calculate(List.of(), new BigDecimal("500"), TODAY).label())
                .isEqualTo(Label.NOT_ENOUGH_DATA);
        assertThat(calculator.calculate(flat(30, 1000), null, TODAY).label())
                .isEqualTo(Label.NOT_ENOUGH_DATA);
    }

    // One point per day from `days` days ago up to today
    private static List<DailyPrice> flat(int days, int price) {
        List<DailyPrice> points = new ArrayList<>();
        for (int i = days; i >= 0; i--) {
            points.add(new DailyPrice(TODAY.minusDays(i), BigDecimal.valueOf(price)));
        }
        return points;
    }
}
